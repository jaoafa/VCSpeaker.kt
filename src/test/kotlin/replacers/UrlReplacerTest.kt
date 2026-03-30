package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.actions.GuildAction.getEntity
import com.jaoafa.vcspeaker.database.actions.ReadableChannelAction
import com.jaoafa.vcspeaker.database.actions.ReadableChannelAction.isReadableChannel
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.GuildTable
import com.jaoafa.vcspeaker.database.tables.ReadableChannelEntity
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.models.original.discord.DiscordInvite
import com.jaoafa.vcspeaker.models.original.twitter.Tweet
import com.jaoafa.vcspeaker.models.response.steam.SteamAppDetail
import com.jaoafa.vcspeaker.models.response.steam.SteamAppDetailData
import com.jaoafa.vcspeaker.models.response.youtube.YouTubeOEmbedResponse
import com.jaoafa.vcspeaker.tools.Steam
import com.jaoafa.vcspeaker.tools.Twitter
import com.jaoafa.vcspeaker.tools.YouTube
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.UrlReplacer
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.ClientResources
import dev.kord.core.behavior.channel.TextChannelBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.GuildScheduledEvent
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL
import utils.createGuildMockk

class UrlReplacerTest : FunSpec({
    val mockedGuildId = Snowflake(123456789012345678)
    val mockedChannelId = Snowflake(876543210987654321)
    val mockedInviterId = Snowflake(2)
    val mockedEventId = Snowflake(3)
    val mockedMessageId = Snowflake(123456789012345678)
    // テスト前に早期にモックを初期化
    beforeSpec {
        DatabaseUtil.init(TEST_DB_MEM_URL)
        DatabaseUtil.createTables()
        mockkObject(VCSpeaker)
    }

    // テスト前にモックを初期化
    beforeEach {
        mockkObject(ReadableChannelAction)
        transaction {
            GuildEntity.new(id = mockedGuildId) {
                this.speakerVoiceEntity = VoiceEntity.new { }
            }
        }
    }

    // テスト後にモックを削除
    afterEach {
        transaction {
            GuildTable.deleteAll()
        }
        clearAllMocks()
    }

    // メッセージURLの置き換え
    context("Make message URLs readable.") {
        // 既知の通常のメッセージURLを置き換える
        test("URL(s) to another message(s) on known server's channel should be replaced with readable text.") {
            // ReadableChannelActionをモック化して、常にfalseを返すようにする
            every { any<TextChannelBehavior>().isReadableChannel() } returns false

            listOf(
                "test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId",
                "test https://discordapp.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId",
                "test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId?query=example",
                "test https://discordapp.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId?query=example",
            ).forEach { text ->
                mapOf(
                    ChannelType.GuildText to "テキストチャンネル",
                    ChannelType.GuildVoice to "ボイスチャンネル",
                    ChannelType.GuildCategory to "カテゴリ",
                    ChannelType.GuildNews to "ニュースチャンネル",
                ).forEach { (channelType, channelTypeText) ->
                    // GuildTextの場合はTextChannelのモックを作成、それ以外は通常のGuildChannelのモック
                    val channelMock = if (channelType == ChannelType.GuildText) {
                        mockk<TextChannel> {
                            val channel = this
                            every { name } returns "test-channel"
                            every { type } returns channelType
                            every { id } returns mockedChannelId
                            every { supplier } returns mockk {
                                coEvery { getChannel(mockedChannelId) } returns channel
                            }
                            coEvery { getMessageOrNull(any()) } returns null
                        }
                    } else {
                        mockk {
                            val channel = this
                            every { name } returns "test-channel"
                            every { type } returns channelType
                            every { id } returns mockedChannelId
                            every { supplier } returns mockk {
                                coEvery { getChannel(mockedChannelId) } returns channel
                            }
                        }
                    }

                    every { VCSpeaker.kord } returns mockk {
                        every { resources } returns mockk<ClientResources>()
                        coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                            every { id } returns mockedGuildId
                            coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                            every { supplier } returns mockk {
                                coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                            }
                        }
                    }

                    val message = mockk<Message>()
                    coEvery { message.getGuild() } returns mockk {
                        every { id } returns mockedGuildId
                    }

                    val tokens = mutableListOf(TextToken(text))
                    val expectedTokens =
                        mutableListOf(TextToken("test ${channelTypeText}「test-channel」で送信したメッセージのリンク"))

                    val processedTokens = UrlReplacer.replace(
                        tokens, mockedGuildId
                    )

                    processedTokens shouldBe expectedTokens
                }
            }
        }

        // 既知のスレッドチャンネルメッセージURLを置き換える
        test("URL(s) to another message(s) on known thread channel should be replaced with readable text.") {
            // ReadableChannelActionをモック化して、常にfalseを返すようにする
            every { any<TextChannelBehavior>().isReadableChannel() } returns false

            mapOf(
                ChannelType.GuildText to "テキストチャンネル",
                ChannelType.GuildNews to "ニュースチャンネル",
            ).forEach { (channelType, channelTypeText) ->
                val parentChannelMock = mockk<TextChannel> {
                    val parent = this
                    every { name } returns "test-thread-parent-channel"
                    every { type } returns channelType
                    every { supplier } returns mockk {
                        coEvery { getChannel(any()) } returns parent
                    }
                }

                val threadChannelMock = mockk<ThreadChannel> {
                    val thread = this
                    every { name } returns "test-thread-channel"
                    every { type } returns ChannelType.PublicGuildThread
                    every { id } returns mockedChannelId
                    every { supplier } returns mockk {
                        coEvery { getChannel(any()) } returns thread
                    }
                    coEvery { asChannelOf<ThreadChannel>() } returns mockk {
                        every { name } returns "test-thread-channel"
                        every { type } returns channelType
                        every { parent } returns mockk {
                            every { supplier } returns mockk {
                                coEvery { getChannel(any()) } returns parentChannelMock
                            }
                            coEvery { asChannel() } returns parentChannelMock
                        }
                    }
                }

                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>()
                    coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                        every { id } returns mockedGuildId
                        coEvery { getChannelOrNull(mockedChannelId) } returns threadChannelMock
                        every { supplier } returns mockk {
                            coEvery { getChannelOrNull(mockedChannelId) } returns threadChannelMock
                        }
                    }
                }

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens =
                    mutableListOf(TextToken("test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId"))
                val expectedTokens =
                    mutableListOf(TextToken("test ${channelTypeText}「test-thread-parent-channel」のスレッド「test-thread-channel」で送信したメッセージのリンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, Snowflake(0)
                )

                processedTokens shouldBe expectedTokens
            }
        }

        // 未知のチャンネルメッセージURLを置き換える
        test("URL(s) to another message(s) on unknown channel should be replaced with readable text.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(mockedGuildId) } returns null
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId"))
            val expectedTokens = mutableListOf(TextToken("test どこかのチャンネルで送信したメッセージのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, mockedGuildId
            )

            processedTokens shouldBe expectedTokens
        }

        // ReadableChannelActionに登録されているチャンネルのメッセージ内容を含むテキストに置き換える
        test("URL(s) to message(s) on readable channel should include message content and author.") {
            // ReadableChannelActionのモックを解除して実際のストアを使う
            unmockkObject(ReadableChannelAction)
            val guild = createGuildMockk(mockedGuildId)

            val authorMock = mockk<User> {
                every { username } returns "TestUser"
            }

            val messageMock = mockk<Message> {
                every { author } returns authorMock
                every { content } returns "これはテストメッセージです"
            }

            val channelMock = mockk<TextChannel>(relaxed = true) {
                every { name } returns "test-channel"
                every { type } returns ChannelType.GuildText
                every { id } returns mockedChannelId
                every { guildId } returns mockedGuildId
                coEvery { getMessageOrNull(mockedMessageId) } returns messageMock
            }

            // ReadableChannelActionに直接追加
            transaction {
                ReadableChannelEntity.new {
                    this.guildEntity = guild.getEntity()
                    channelDid = mockedChannelId
                    creatorDid = Snowflake(0)
                }
            }

            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>()
                coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                    every { id } returns mockedGuildId
                    coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    every { supplier } returns mockk {
                        coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    }
                }
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId"))
            val expectedTokens =
                mutableListOf(TextToken("test テキストチャンネル「test-channel」でユーザー「TestUser」が送信したメッセージ「これはテストメッセージです」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, mockedGuildId
            )

            processedTokens shouldBe expectedTokens
        }

        // ReadableChannelActionに登録されているが、メッセージ内容が長い場合は180文字で切り詰める
        test("URL(s) to message(s) on readable channel with long content should be truncated at 180 code points.") {
            unmockkObject(ReadableChannelAction)
            val guild = createGuildMockk(mockedGuildId)

            val authorMock = mockk<User> {
                every { username } returns "TestUser"
            }

            val longContent = "あ".repeat(200)
            val messageMock = mockk<Message> {
                every { author } returns authorMock
                every { content } returns longContent
            }

            val channelMock = mockk<TextChannel>(relaxed = true) {
                every { name } returns "test-channel"
                every { type } returns ChannelType.GuildText
                every { id } returns mockedChannelId
                every { guildId } returns mockedGuildId
                coEvery { getMessageOrNull(mockedMessageId) } returns messageMock
            }

            transaction {
                ReadableChannelEntity.new {
                    this.guildEntity = guild.getEntity()
                    channelDid = mockedChannelId
                    creatorDid = Snowflake(0)
                }
            }

            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>()
                coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                    every { id } returns mockedGuildId
                    coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    every { supplier } returns mockk {
                        coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    }
                }
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId"))
            val expectedContent = "あ".repeat(180) + " 以下略"
            val expectedTokens =
                mutableListOf(TextToken("test テキストチャンネル「test-channel」でユーザー「TestUser」が送信したメッセージ「$expectedContent」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, mockedGuildId
            )

            processedTokens shouldBe expectedTokens
        }

        // ReadableChannelActionに登録されているが、メッセージ内容が空の場合のフォールバック
        test("URL(s) to message(s) on readable channel with empty content should show fallback text.") {
            unmockkObject(ReadableChannelAction)
            val guild = createGuildMockk(mockedGuildId)

            val authorMock = mockk<User> {
                every { username } returns "TestUser"
            }

            val messageMock = mockk<Message> {
                every { author } returns authorMock
                every { content } returns ""
            }

            val channelMock = mockk<TextChannel>(relaxed = true) {
                every { name } returns "test-channel"
                every { type } returns ChannelType.GuildText
                every { id } returns mockedChannelId
                every { guildId } returns mockedGuildId
                coEvery { getMessageOrNull(mockedMessageId) } returns messageMock
            }

            transaction {
                ReadableChannelEntity.new {
                    this.guildEntity = guild.getEntity()
                    channelDid = mockedChannelId
                    creatorDid = Snowflake(0)
                }
            }

            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>()
                coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                    every { id } returns mockedGuildId
                    coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    every { supplier } returns mockk {
                        coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    }
                }
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId"))
            val expectedTokens =
                mutableListOf(TextToken("test テキストチャンネル「test-channel」でユーザー「TestUser」が送信したメッセージ「添付ファイルのみのメッセージ」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, mockedGuildId
            )

            processedTokens shouldBe expectedTokens
        }

        // ReadableChannelActionに登録されているが、作者が不明な場合のフォールバック
        test("URL(s) to message(s) on readable channel with unknown author should show fallback text.") {
            unmockkObject(ReadableChannelAction)
            val guild = createGuildMockk(mockedGuildId)

            val messageMock = mockk<Message> {
                every { author } returns null
                every { content } returns "テストメッセージ"
            }

            val channelMock = mockk<TextChannel>(relaxed = true) {
                every { name } returns "test-channel"
                every { type } returns ChannelType.GuildText
                every { id } returns mockedChannelId
                every { guildId } returns mockedGuildId
                coEvery { getMessageOrNull(mockedMessageId) } returns messageMock
            }

            transaction {
                ReadableChannelEntity.new {
                    this.guildEntity = guild.getEntity()
                    channelDid = mockedChannelId
                    creatorDid = Snowflake(0)
                }
            }

            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>()
                coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                    every { id } returns mockedGuildId
                    coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    every { supplier } returns mockk {
                        coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    }
                }
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId"))
            val expectedTokens =
                mutableListOf(TextToken("test テキストチャンネル「test-channel」でユーザー「不明なユーザー」が送信したメッセージ「テストメッセージ」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, mockedGuildId
            )

            processedTokens shouldBe expectedTokens
        }

        // ReadableChannelAction に登録済みでもメッセージ取得が失敗した場合は従来の読み上げにフォールバックする
        test("URL(s) to message(s) on readable channel when message not found should fall back to channel name only.") {
            unmockkObject(ReadableChannelAction)
            val guild = createGuildMockk(mockedGuildId)

            val channelMock = mockk<TextChannel>(relaxed = true) {
                every { name } returns "test-channel"
                every { type } returns ChannelType.GuildText
                every { id } returns mockedChannelId
                // メッセージが見つからない場合
                coEvery { getMessageOrNull(any()) } returns null
            }

            transaction {
                ReadableChannelEntity.new {
                    this.guildEntity = guild.getEntity()
                    channelDid = mockedChannelId
                    creatorDid = Snowflake(0)
                }
            }

            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>()
                coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                    every { id } returns mockedGuildId
                    coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    every { supplier } returns mockk {
                        coEvery { getChannelOrNull(mockedChannelId) } returns channelMock
                    }
                }
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/channels/$mockedGuildId/$mockedChannelId/$mockedMessageId"))
            val expectedTokens =
                mutableListOf(TextToken("test テキストチャンネル「test-channel」で送信したメッセージのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, mockedGuildId
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // チャンネルURLの置き換え
    context("replaceChannelUrl") {
        // 既知の通常のチャンネルURLを置き換える
        test("URL(s) to known channel(s) should be replaced with readable text.") {
            listOf(
                "test https://discord.com/channels/123456789012345678/876543210987654321",
                "test https://discordapp.com/channels/123456789012345678/876543210987654321",
                "test https://discord.com/channels/123456789012345678/876543210987654321?query=example",
                "test https://discordapp.com/channels/123456789012345678/876543210987654321?query=example",
            ).forEach { text ->
                mapOf(
                    ChannelType.GuildText to "テキストチャンネル",
                    ChannelType.GuildVoice to "ボイスチャンネル",
                    ChannelType.GuildCategory to "カテゴリ",
                    ChannelType.GuildNews to "ニュースチャンネル",
                ).forEach { (channelType, channelTypeText) ->
                    every { VCSpeaker.kord } returns mockk {
                        every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                        coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                            coEvery { getChannelOrNull(mockedChannelId) } returns mockk {
                                every { name } returns "test-channel" // テスト用のチャンネル名
                                every { type } returns channelType
                            }
                        }
                    }

                    val message = mockk<Message>()
                    coEvery { message.getGuild() } returns mockk {
                        every { id } returns mockedGuildId
                    }

                    val tokens = mutableListOf(TextToken(text))
                    val expectedTokens = mutableListOf(TextToken("test ${channelTypeText}「test-channel」へのリンク"))

                    val processedTokens = UrlReplacer.replace(
                        tokens, Snowflake(0)
                    )

                    processedTokens shouldBe expectedTokens
                }
            }
        }

        // 既知のスレッドチャンネルURLを置き換える
        test("URL(s) to known thread channel(s) should be replaced with readable text.") {
            mapOf(
                ChannelType.GuildText to "テキストチャンネル",
                ChannelType.GuildNews to "ニュースチャンネル",
            ).forEach { (channelType, channelTypeText) ->
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                    coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                        coEvery { getChannelOrNull(mockedChannelId) } returns mockk<ThreadChannel> {
                            every { name } returns "test-thread-channel" // テスト用のスレッドチャンネル名
                            every { type } returns ChannelType.PublicGuildThread
                            coEvery { asChannelOf<ThreadChannel>() } returns mockk {
                                every { name } returns "test-thread-channel" // テスト用のスレッドチャンネル名
                                every { type } returns channelType
                                every { parent } returns mockk {
                                    coEvery { asChannel() } returns mockk {
                                        every { name } returns "test-thread-parent-channel" // テスト用のチャンネル名
                                        every { type } returns channelType
                                    }
                                }
                            }
                        }
                    }
                }

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens =
                    mutableListOf(TextToken("test https://discord.com/channels/123456789012345678/876543210987654321"))
                val expectedTokens =
                    mutableListOf(TextToken("test ${channelTypeText}「test-thread-parent-channel」のスレッド「test-thread-channel」へのリンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, Snowflake(0)
                )

                processedTokens shouldBe expectedTokens
            }
        }

        // 未知のチャンネルURLを置き換える
        test("URL(s) to unknown channel(s) should be replaced with readable text.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(mockedGuildId) } returns null
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/channels/123456789012345678/876543210987654321"))
            val expectedTokens = mutableListOf(TextToken("test どこかのチャンネルへのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // イベントへの直接URLの置き換え
    context("Make direct event URLs readable.") {
        // メッセージが投稿されたサーバでのイベントへのリンクを置き換える
        test("URL(s) to Event on the guild should be replaced with readable text.") {
            listOf(
                "test https://discord.com/events/123456789012345678/876543210987654321",
                "test https://discordapp.com/events/123456789012345678/876543210987654321",
                "test https://discord.com/events/123456789012345678/876543210987654321?query=example",
                "test https://discordapp.com/events/123456789012345678/876543210987654321?query=example",
            ).forEach { text ->
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                    coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                        every { id } returns mockedGuildId
                        every { scheduledEvents } returns flow {
                            emit(mockk<GuildScheduledEvent> {
                                every { id } returns mockedChannelId
                                every { name } returns "test-event" // テスト用のイベント名
                            })
                        }
                    }
                }

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens = mutableListOf(TextToken(text))
                val expectedTokens = mutableListOf(TextToken("test イベント「test-event」へのリンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, mockedGuildId
                )

                processedTokens shouldBe expectedTokens
            }
        }

        // 他のサーバでのイベントへのリンクを置き換える
        test("URL(s) to Event on external guild(s) should be replaced with readable text.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                    every { id } returns mockedGuildId
                    every { name } returns "test-guild" // テスト用のサーバ名
                    every { scheduledEvents } returns flow {
                        emit(mockk<GuildScheduledEvent> {
                            every { id } returns mockedChannelId
                            every { name } returns "test-event" // テスト用のイベント名
                        })
                    }
                }
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(123789456012345678)
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/events/123456789012345678/876543210987654321"))
            val expectedTokens = mutableListOf(TextToken("test サーバ「test-guild」のイベント「test-event」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(123789456012345678)
            )

            processedTokens shouldBe expectedTokens
        }

        // 既知のサーバだが未知のイベントへのリンクを置き換える
        test("URL(s) to unknown Event on known guild(s) should be replaced with readable text.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(mockedGuildId) } returns mockk {
                    every { id } returns mockedGuildId
                    every { name } returns "test-guild" // テスト用のサーバ名
                    every { scheduledEvents } returns flow {}
                }
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(123789456012345678)
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/events/123456789012345678/876543210987654321"))
            val expectedTokens = mutableListOf(TextToken("test サーバ「test-guild」のイベントへのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(123789456012345678)
            )

            processedTokens shouldBe expectedTokens
        }

        // 未知のサーバでのイベントへのリンクを置き換える
        test("URL(s) to Event on unknown guild(s) should be replaced with readable text.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(mockedGuildId) } returns null
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens =
                mutableListOf(TextToken("test https://discord.com/events/123456789012345678/876543210987654321"))
            val expectedTokens = mutableListOf(TextToken("test どこかのサーバのイベントへのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // イベント招待URLの置き換え
    context("Make event invitation URLs readable.") {
        // メッセージが投稿されたサーバでのイベントへのリンクを置き換える
        test("If events are on the guild.") {
            listOf(
                "test https://discord.com/invite/abcdef?event=123456789012345678",
                "test https://discordapp.com/invite/abcdef?event=123456789012345678",
                "test https://discord.com/invite/abcdef?event=123456789012345678&query=example",
                "test https://discordapp.com/invite/abcdef?event=123456789012345678&query=example",
                "test https://discord.gg/abcdef?event=123456789012345678",
                "test https://discord.gg/abcdef?event=123456789012345678&query=example",
                "test discord.com/invite/abcdef?event=123456789012345678",
                "test discordapp.com/invite/abcdef?event=123456789012345678",
                "test discord.gg/abcdef?event=123456789012345678",
                "test discord.gg/abcdef?event=123456789012345678&query=example",
            ).forEach { text ->
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                }

                mockkObject(UrlReplacer)
                coEvery { UrlReplacer["getInvite"]("abcdef", mockedGuildId) } returns DiscordInvite(
                    code = "abcdef",
                    guildId = mockedGuildId,
                    guildName = "test-guild",
                    channelId = mockedChannelId,
                    channelName = "test-channel",
                    inviterId = mockedInviterId,
                    inviterName = "test-user",
                    eventId = mockedEventId,
                    eventName = "test-event",
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens = mutableListOf(TextToken(text))
                val expectedTokens = mutableListOf(TextToken("test イベント「test-event」へのリンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, mockedGuildId
                )

                processedTokens shouldBe expectedTokens
            }
        }

        // 他のサーバでのイベントへのリンクを置き換える
        test("If events are on another guilds.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
            }


            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getInvite"]("abcdef", mockedGuildId) } returns DiscordInvite(
                code = "abcdef",
                guildId = mockedGuildId,
                guildName = "test-guild",
                channelId = mockedChannelId,
                channelName = "test-channel",
                inviterId = mockedInviterId,
                inviterName = "test-user",
                eventId = mockedEventId,
                eventName = "test-event",
            )

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(123789456012345678)
            }

            val tokens = mutableListOf(TextToken("test https://discord.com/invite/abcdef?event=123456789012345678"))
            val expectedTokens = mutableListOf(TextToken("test サーバ「test-guild」のイベント「test-event」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(123789456012345678)
            )

            processedTokens shouldBe expectedTokens
        }

        // 招待リンクが取得できなかった場合の置き換え
        test("If invitation details could not be retrieved, replace it as unknown invite.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
            }

            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getInvite"]("abcdef", mockedGuildId) } returns null

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(123789456012345678)
            }

            val tokens = mutableListOf(TextToken("test https://discord.com/invite/abcdef?event=123456789012345678"))
            val expectedTokens = mutableListOf(TextToken("test どこかのサーバのイベントへのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(123789456012345678)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // ツイートURLの置き換え
    context("Make tweet URLs readable.") {
        // 存在するツイートで、短いツイートの場合
        test("If the tweet exists and short enough, read whole tweet.") {
            listOf(
                "test https://twitter.com/username/status/123456789012345678",
                "test https://twitter.com/username/status/123456789012345678?query=example",
                "test https://twitter.com/username/status/123456789012345678/",
                "test https://twitter.com/username/status/123456789012345678/?query=example",
                "test https://x.com/username/status/123456789012345678",
                "test https://x.com/username/status/123456789012345678?query=example",
                "test https://x.com/username/status/123456789012345678/",
                "test https://x.com/username/status/123456789012345678/?query=example",
            ).forEach { text ->
                mockkObject(Twitter)
                coEvery { Twitter.getTweet("username", "123456789012345678") } returns Tweet(
                    authorName = "test-user ⚠️",
                    html = "<p>test-tweet</p>",
                    plainText = "test-plaintext",
                    readText = "test-readtext",
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens = mutableListOf(TextToken(text))
                val expectedTokens = mutableListOf(TextToken("test test-user のツイート「test-readtext」へのリンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, Snowflake(0)
                )

                processedTokens shouldBe expectedTokens
            }
        }

        // 存在するツイートで、長いツイートの場合
        test("If the tweet exists but too long to read, read first 70 characters.") {
            mockkObject(Twitter)
            coEvery { Twitter.getTweet("username", "123456789012345678") } returns Tweet(
                authorName = "test-user ⚠️",
                html = "<p>test-tweet</p>",
                plainText = "long".repeat(100),
                readText = "long".repeat(100),
            )

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens = mutableListOf(TextToken("test https://twitter.com/username/status/123456789012345678"))
            val expectedTokens =
                mutableListOf(TextToken("test test-user のツイート「longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglo 以下略」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }

        // 存在するツイートで、特殊文字を含む長いツイートの場合
        test("If the tweet exists but too long to read, read first 70 characters with special characters.") {
            mockkObject(Twitter)
            coEvery { Twitter.getTweet("username", "123456789012345678") } returns Tweet(
                authorName = "test-user ⚠️",
                html = "<p>test-tweet</p>",
                plainText = "𝚐".repeat(100), // 普通の g ではない
                readText = "𝚐".repeat(100),
            )

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens = mutableListOf(TextToken("test https://twitter.com/username/status/123456789012345678"))
            val expectedTokens =
                mutableListOf(TextToken("test test-user のツイート「𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐𝚐 以下略」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }

        // 存在しないツイートの場合
        test("If the tweet not found, read it as unknown tweet.") {
            mockkObject(Twitter)
            coEvery { Twitter.getTweet("username", "123456789012345678") } returns null

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens = mutableListOf(TextToken("test https://twitter.com/username/status/123456789012345678"))
            val expectedTokens = mutableListOf(TextToken("test ユーザー「username」のツイートへのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // 招待URLの置き換え
    context("Make invitation URLs readable.") {
        // メッセージが投稿されたサーバでの招待リンクを置き換える
        test("If the invite is for a channel in the guild, read the name of the channel.") {
            listOf(
                "test https://discord.com/invite/abcdef",
                "test https://discordapp.com/invite/abcdef",
                "test https://discord.com/invite/abcdef?query=example",
                "test https://discordapp.com/invite/abcdef?query=example",
                "test https://discord.gg/abcdef",
                "test https://discord.gg/abcdef?query=example",
                "test discord.com/invite/abcdef",
            ).forEach { text ->
                mockkObject(UrlReplacer)
                coEvery { UrlReplacer["getInvite"]("abcdef", any<Snowflake>()) } returns DiscordInvite(
                    code = "abcdef",
                    guildId = mockedGuildId,
                    guildName = "test-guild",
                    channelId = mockedChannelId,
                    channelName = "test-channel",
                    inviterId = mockedInviterId,
                    inviterName = "test-user",
                    eventId = mockedEventId,
                    eventName = "test-event",
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens = mutableListOf(TextToken(text))
                val expectedTokens = mutableListOf(TextToken("test チャンネル「test-channel」への招待リンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, mockedGuildId
                )

                processedTokens shouldBe expectedTokens
            }
        }

        // 他のサーバでの招待リンクを置き換える
        test("If the invite is for another guild, also read the name of the guild.") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getInvite"]("abcdef", any<Snowflake>()) } returns DiscordInvite(
                code = "abcdef",
                guildId = mockedGuildId,
                guildName = "test-guild",
                channelId = mockedChannelId,
                channelName = "test-channel",
                inviterId = mockedInviterId,
                inviterName = "test-user",
                eventId = mockedEventId,
                eventName = "test-event",
            )

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(123789456012345678)
            }

            val tokens = mutableListOf(TextToken("test https://discord.com/invite/abcdef"))
            val expectedTokens =
                mutableListOf(TextToken("test サーバ「test-guild」のチャンネル「test-channel」への招待リンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(123789456012345678)
            )

            processedTokens shouldBe expectedTokens
        }

        // 招待リンクが取得できなかった場合の置き換え
        test("If the invitation details could not be retrieved, read it as unknown invite.") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getInvite"]("abcdef", any<Snowflake>()) } returns null

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens = mutableListOf(TextToken("test https://discord.com/invite/abcdef"))
            val expectedTokens = mutableListOf(TextToken("test どこかのサーバへの招待リンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // SteamアプリURLの置き換え
    context("Make Steam Store URLs readable.") {
        // 存在するアプリの場合
        test("If the app exists, read its name.") {
            listOf(
                "test https://store.steampowered.com/app/1234567890",
                "test https://store.steampowered.com/app/1234567890?query=example",
            ).forEach { text ->
                mockkObject(Steam)
                coEvery { Steam.getAppDetail("1234567890") } returns SteamAppDetail(
                    success = true, data = SteamAppDetailData(
                        type = "game",
                        name = "test-app",
                    )
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens = mutableListOf(TextToken(text))
                val expectedTokens = mutableListOf(TextToken("test Steamアイテム「test-app」へのリンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, Snowflake(0)
                )

                processedTokens shouldBe expectedTokens
            }
        }

        // 存在しないアプリの場合
        test("If the app not found, read it as unknown app.") {
            mockkObject(Steam)
            coEvery { Steam.getAppDetail("1234567890") } returns null

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens = mutableListOf(TextToken("test https://store.steampowered.com/app/1234567890"))
            val expectedTokens = mutableListOf(TextToken("test Steamアイテムへのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // YouTubeURLの置き換え
    context("Make YouTube URLs readable.") {
        // 存在する動画の場合
        test("If the video exists, read its title and author.") {
            mapOf(
                "test https://www.youtube.com/watch?v=abcdefg" to "動画",
                "test http://youtube.com/watch?v=abcdefg" to "動画",
                "test https://m.youtube.com/watch?v=abcdefg" to "動画",
                "test youtu.be/abcdefg" to "動画",
                "test www.youtube.com/embed/abcdefg" to "動画",
                "test youtube-nocookie.com/embed/abcdefg" to "動画",
                "test https://youtube.com/v/abcdefg" to "動画",
                "test https://youtube.com/e/abcdefg" to "動画",
                "test https://youtube.com/shorts/abcdefg" to "ショート",
                "test https://youtube.com/live/abcdefg" to "配信",
                "test https://www.youtube.com/watch.php?v=abcdefg" to "動画",
                "test http://www.youtube.com/watch?v=abcdefg&feature=related" to "動画",
                "test https://www.youtube.com/watch?v=abcdefg#t=30s" to "動画",
                "test https://www.youtube.com/watch?v=abcdefg&ab_channel=TestChannel" to "動画",
                "test http://youtube.com/watch?v=abcdefg&list=PLAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" to "動画",
            ).forEach { (text, type) ->
                mockkObject(YouTube)
                coEvery { YouTube.getVideo("abcdefg") } returns YouTubeOEmbedResponse(
                    authorName = "test-user",
                    authorUrl = "https://www.youtube.com/channel/UCabcdefg",
                    height = 720,
                    html = "<iframe src=\"https://www.youtube.com/embed/abcdefg\"></iframe>",
                    providerName = "YouTube",
                    providerUrl = "https://www.youtube.com/",
                    thumbnailHeight = 360,
                    thumbnailUrl = "https://i.ytimg.com/vi/abcdefg/hqdefault.jpg",
                    thumbnailWidth = 480,
                    title = "test-video",
                    type = "video",
                    version = "1.0",
                    width = 1280,
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens = mutableListOf(TextToken(text))
                val expectedTokens =
                    mutableListOf(TextToken("test YouTubeの「test-user」による${type}「test-video」へのリンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, Snowflake(0)
                )

                processedTokens shouldBe expectedTokens
            }
        }

        // 存在する動画だが、タイトルや作者名が長い場合
        test("If the video exists, read its title and author but truncated.") {
            mockkObject(YouTube)
            coEvery { YouTube.getVideo("abcdefg") } returns YouTubeOEmbedResponse(
                authorName = "test-user".repeat(100),
                authorUrl = "https://www.youtube.com/channel/UCabcdefg",
                height = 720,
                html = "<iframe src=\"https://www.youtube.com/embed/abcdefg\"></iframe>",
                providerName = "YouTube",
                providerUrl = "https://www.youtube.com/",
                thumbnailHeight = 360,
                thumbnailUrl = "https://i.ytimg.com/vi/abcdefg/hqdefault.jpg",
                thumbnailWidth = 480,
                title = "test-video".repeat(100),
                type = "video",
                version = "1.0",
                width = 1280,
            )

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens = mutableListOf(TextToken("test https://www.youtube.com/watch?v=abcdefg"))
            val expectedTokens =
                mutableListOf(TextToken("test YouTubeの「test-usertest-u 以下略」による動画「test-videotest-video 以下略」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // YouTubeプレイリストURLの置き換え
    context("Make YouTube playlist URLs readable.") {
        // 存在するプレイリストの場合
        test("If the playlist exists, read its title and author.") {
            listOf(
                "test https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
                "test http://youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
                "test https://m.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
                "test youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
                "test www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI&feature=share",
                "test m.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
                "test https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI#t=30s",
                "test http://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI&index=5",
                "test https://youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI&ab_channel=RickAstley",
                "test http://m.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI&shuffle=1",
            ).forEach { text ->
                mockkObject(YouTube)
                coEvery { YouTube.getPlaylist("PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI") } returns YouTubeOEmbedResponse(
                    authorName = "test-user",
                    authorUrl = "https://www.youtube.com/channel/UCabcdefg",
                    height = 720,
                    html = "<iframe src=\"https://www.youtube.com/embed/abcdefg\"></iframe>",
                    providerName = "YouTube",
                    providerUrl = "https://www.youtube.com/",
                    thumbnailHeight = 360,
                    thumbnailUrl = "https://i.ytimg.com/vi/abcdefg/hqdefault.jpg",
                    thumbnailWidth = 480,
                    title = "test-playlist",
                    type = "playlist",
                    version = "1.0",
                    width = 1280,
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns mockedGuildId
                }

                val tokens = mutableListOf(TextToken(text))
                val expectedTokens =
                    mutableListOf(TextToken("test YouTubeの「test-user」によるプレイリスト「test-playlist」へのリンク"))

                val processedTokens = UrlReplacer.replace(
                    tokens, Snowflake(0)
                )

                processedTokens shouldBe expectedTokens
            }
        }
    }

    // Google検索URLの置き換え
    context("Make Google Search URLs readable.") {
        // 通常の検索URL
        test("If the URL contains no special characters, just read it.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens = mutableListOf(TextToken("test https://www.google.com/search?q=example"))
            val expectedTokens = mutableListOf(TextToken("test Google検索「example」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }

        // 日本語文字列の検索URL (URLエンコードされている文字列)
        test("If the URL contains Javascript, decode the URL and read it.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns mockedGuildId
            }

            val tokens =
                mutableListOf(TextToken("test https://www.google.com/search?q=%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF"))
            val expectedTokens = mutableListOf(TextToken("test Google検索「こんにちは」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // URLからtitleタグ値への置き換え
    context("Make URLs readable.") {
        // 単一のURL
        test("If the content contains only a single URL, read its title.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val tokens = mutableListOf(TextToken("https://example.com"))
            val expectedTokens = mutableListOf(TextToken("Webページ「Example Domain」へのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }

        // 単一のURLにテキストが付随
        test("If the content contains a single URL, read its title.") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getPageTitle"]("https://example.com") } returns "Example Domain"

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val tokens = mutableListOf(TextToken("Please visit https://example.com for more information."))
            val expectedTokens =
                mutableListOf(TextToken("Please visit Webページ「Example Domain」へのリンク for more information."))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }

        // 複数のURL
        test("If the content contains multiple URLs, read their title.") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getPageTitle"]("https://example.com") } returns "Example Domain"
            coEvery { UrlReplacer["getPageTitle"]("https://www.iana.org/help/example-domains") } returns "Example Domains"

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val tokens =
                mutableListOf(TextToken("https://www.iana.org/help/example-domains explains why https://example.com is reserved."))
            val expectedTokens =
                mutableListOf(TextToken("Webページ「Example Domains」へのリンク explains why Webページ「Example Domain」へのリンク is reserved."))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }

        // 存在しないドメイン
        test("If the domain does not exist, read it as non-existent website.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val tokens = mutableListOf(TextToken("test https://example.invalid")) // RFC 2606
            val expectedTokens = mutableListOf(TextToken("test 存在しないWebページへのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // URLを拡張子に置き換える
    context("Make URLs with extension readable.") {
        // 定義された拡張子を持つURL
        test("If the URL has known extension, read its readable name.") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getPageTitle"]("https://example.com/test.jpg") } returns null

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val tokens = mutableListOf(TextToken("test https://example.com/test.jpg"))
            val expectedTokens = mutableListOf(TextToken("test JPEGファイルへのリンク"))

            val processedTokens = UrlReplacer.replace(
                tokens, Snowflake(0)
            )

            processedTokens shouldBe expectedTokens
        }
    }

    // 未定義の拡張子を持つURL
    test("If the URL has unknown extension, read its name.") {
        mockkObject(UrlReplacer)
        coEvery { UrlReplacer["getPageTitle"]("https://example.com/test.hoge") } returns null

        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val tokens = mutableListOf(TextToken("test https://example.com/test.hoge"))
        val expectedTokens = mutableListOf(TextToken("test hogeファイルへのリンク"))

        val processedTokens = UrlReplacer.replace(
            tokens, Snowflake(0)
        )

        processedTokens shouldBe expectedTokens
    }

    // 拡張子を持たないURL
    test("If the URL doesn't have extension, read its title.") {
        mockkObject(UrlReplacer)
        coEvery { UrlReplacer["getPageTitle"]("https://example.com/test") } returns null

        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val tokens = mutableListOf(TextToken("test https://example.com/test"))
        val expectedTokens = mutableListOf(TextToken("test Webページのリンク"))

        val processedTokens = UrlReplacer.replace(
            tokens, Snowflake(0)
        )

        processedTokens shouldBe expectedTokens
    }
})
