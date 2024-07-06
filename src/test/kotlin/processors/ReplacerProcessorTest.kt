package processors

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.models.original.discord.DiscordInvite
import com.jaoafa.vcspeaker.models.original.twitter.Tweet
import com.jaoafa.vcspeaker.models.response.steam.SteamAppDetail
import com.jaoafa.vcspeaker.models.response.steam.SteamAppDetailData
import com.jaoafa.vcspeaker.models.response.youtube.YouTubeOEmbedResponse
import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tools.Steam
import com.jaoafa.vcspeaker.tools.Twitter
import com.jaoafa.vcspeaker.tools.YouTube
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.ReplacerProcessor
import com.jaoafa.vcspeaker.tts.replacers.UrlReplacer
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.ClientResources
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.GuildScheduledEvent
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.thread.ThreadChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * ReplacerProcessor のテスト
 */
class ReplacerProcessorTest : FunSpec({
    // テスト前にモックを初期化
    beforeTest {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker")

        val storeStruct = mockk<StoreStruct<IgnoreData>>()
        every { storeStruct.write() } returns Unit

        mockkObject(IgnoreStore)
        every { IgnoreStore.write() } returns Unit

        IgnoreStore.data.clear()

        mockkObject(AliasStore)
        every { AliasStore.write() } returns Unit

        AliasStore.data.clear()
    }

    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // 全てのテスト後にフォルダを削除
    finalizeSpec {
        VCSpeaker.storeFolder.deleteRecursively()
    }

    // テキストが変更されないことを確認
    test("If nothing is configured, the text should remain unchanged.") {
        val text = "Hello, world!"
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)
        processedText shouldBe text
        processedVoice shouldBe voice
    }

    // エイリアスのテスト
    context("alias") {
        // テキストエイリアスを設定した場合、正しく置き換えられる
        test("If a text alias matches the message, the replaced text should be returned.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Text,
                    search = "world",
                    replace = "Kotlin"
                )
            )

            val (processedText, processedVoice) = ReplacerProcessor().process(message, "Hello, world!", voice)

            processedText shouldBe "Hello, Kotlin!"
            processedVoice shouldBe voice
        }

        // テキストエイリアスを設定していても合致しない場合、変更されない
        test("If a text alias does not match the content, the text should remain unchanged.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Text,
                    search = "Java",
                    replace = "Kotlin"
                )
            )

            val text = "Hello, world!"

            val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)

            processedText shouldBe text
            processedVoice shouldBe voice
        }

        // 正規表現エイリアスを設定した場合、正しく置き換えられる
        test("If a regex alias matches the content, the replaced text should be returned.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Regex,
                    search = "w.+d",
                    replace = "Kotlin"
                )
            )

            val (processedText, processedVoice) = ReplacerProcessor().process(message, "Hello, world!", voice)

            processedText shouldBe "Hello, Kotlin!"
            processedVoice shouldBe voice
        }

        // 正規表現エイリアスを設定していても合致しない場合、変更されない
        test("If a regex alias does not match the content, the text should remain unchanged.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Regex,
                    search = "w.d",
                    replace = "Kotlin"
                )
            )

            val text = "Hello, world!"

            val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)

            processedText shouldBe text
            processedVoice shouldBe voice
        }

        // 複数のエイリアスを設定した場合、正しく置き換えられる
        test("If multiple aliases match the content, the replaced text should be returned.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Text,
                    search = "Hello",
                    replace = "Bonjour"
                )
            )

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Regex,
                    search = "w.+d",
                    replace = "Kotlin"
                )
            )

            val (processedText, processedVoice) = ReplacerProcessor().process(message, "Hello, world!", voice)

            processedText shouldBe "Bonjour, Kotlin!"
            processedVoice shouldBe voice
        }

        // エイリアスは再帰的には行われない
        test("Alias should not match the content recursively.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Text,
                    search = "Hello",
                    replace = "Bonjour"
                )
            )

            // should be skipped
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Text,
                    search = "Bonjour, world!",
                    replace = "你好，Kotlin!"
                )
            )

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Regex,
                    search = "w.+d",
                    replace = "Kotlin"
                )
            )

            val (processedText, processedVoice) = ReplacerProcessor().process(message, "Hello, world!", voice)

            processedText shouldBe "Bonjour, Kotlin!"
            processedVoice shouldBe voice
        }

        // 絵文字エイリアスを設定した場合、正しく置き換えられる
        test("If an emoji alias match the content, the replaced text should be returned.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Emoji,
                    search = "<:world:123456789012345678>",
                    replace = "world"
                )
            )

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, "Hello, <:world:123456789012345678>!", voice
            )

            processedText shouldBe "Hello, world!"
            processedVoice shouldBe voice
        }
    }

    // メンションのテスト
    context("mentions") {
        // 既知のチャンネルメンションを置き換える
        test("Mentions of known channels should be replaced with their associated name.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getChannel(Snowflake(123456789012345678)) } returns mockk {
                    every { data } returns mockk {
                        every { name } returns mockk {
                            every { value } returns "test-channel" // テスト用のチャンネル名
                        }
                    }
                }
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, "Hello, <#123456789012345678>!", voice
            )

            processedText shouldBe "Hello, #test-channel!"
            processedVoice shouldBe voice
        }

        // 未知のチャンネルメンションを置き換える
        test("Mentions of unknown channels should be replaced as unknown channels.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getChannel(Snowflake(123456789012345678)) } returns null
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, "Hello, <#123456789012345678>!", voice
            )

            processedText shouldBe "Hello, #不明なチャンネル!"
            processedVoice shouldBe voice
        }

        // 既知のロールメンションを置き換える
        test("Mentions of known roles should be replaced with its associated name.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(Snowflake(0)) } returns mockk {
                    coEvery { getRole(Snowflake(123456789012345678)) } returns mockk {
                        every { data } returns mockk {
                            every { name } returns "test-role" // テスト用のロール名
                        }
                    }
                }
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, "Hello, <@&123456789012345678>!", voice
            )

            processedText shouldBe "Hello, @test-role!"
            processedVoice shouldBe voice
        }

        // 未知のロールメンションを置き換える
        test("Mentions of unknown roles should be replaced as unknown roles.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(Snowflake(0)) } returns null
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, "Hello, <@&123456789012345678>!", voice
            )

            processedText shouldBe "Hello, @不明なロール!"
            processedVoice shouldBe voice
        }

        // 既知のユーザーメンションを置き換える
        test("Mentions of known users should be replaced with its associated name.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(Snowflake(0)) } returns mockk {
                    coEvery { getMember(Snowflake(123456789012345678)) } returns mockk {
                        every { effectiveName } returns "test-user" // テスト用のユーザー名
                    }
                }
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, "Hello, <@123456789012345678>!", voice
            )

            processedText shouldBe "Hello, @test-user!"
            processedVoice shouldBe voice
        }

        // 未知のユーザーメンションを置き換える
        test("Mentions of unknown users should be replaced as unknown users.") {
            every { VCSpeaker.kord } returns mockk {
                every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                coEvery { getGuildOrNull(Snowflake(0)) } returns null
            }

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, "Hello, <@123456789012345678>!", voice
            )

            processedText shouldBe "Hello, @不明なユーザー!"
            processedVoice shouldBe voice
        }
    }

    // URLのテスト
    context("url") {
        // メッセージURLの置き換え
        context("Make message URLs readable.") {
            // 既知の通常のメッセージURLを置き換える
            test("URL(s) to another message(s) on known server's channel should be replaced with readable text.") {
                listOf(
                    "test https://discord.com/channels/123456789012345678/876543210987654321/123456789012345678",
                    "test https://discordapp.com/channels/123456789012345678/876543210987654321/123456789012345678",
                    "test https://discord.com/channels/123456789012345678/876543210987654321/123456789012345678?query=example",
                    "test https://discordapp.com/channels/123456789012345678/876543210987654321/123456789012345678?query=example",
                ).forEach { text ->
                    mapOf(
                        ChannelType.GuildText to "テキストチャンネル",
                        ChannelType.GuildVoice to "ボイスチャンネル",
                        ChannelType.GuildCategory to "カテゴリ",
                        ChannelType.GuildNews to "ニュースチャンネル",
                    ).forEach { (channelType, channelTypeText) ->
                        every { VCSpeaker.kord } returns mockk {
                            every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                            coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns mockk {
                                coEvery { getChannelOrNull(Snowflake(876543210987654321)) } returns mockk {
                                    every { name } returns "test-channel" // テスト用のチャンネル名
                                    every { type } returns channelType
                                }
                            }
                        }

                        val message = mockk<Message>()
                        coEvery { message.getGuild() } returns mockk {
                            every { id } returns Snowflake(123456789012345678)
                        }
                        val voice = Voice(speaker = Speaker.Hikari)

                        val expected = "test ${channelTypeText}「test-channel」で送信したメッセージのリンク"

                        val (processedText, processedVoice) = ReplacerProcessor().process(
                            message, text, voice
                        )

                        processedText shouldBe expected
                        processedVoice shouldBe voice
                    }
                }
            }

            // 既知のスレッドチャンネルメッセージURLを置き換える
            test("URL(s) to another message(s) on known thread channel should be replaced with readable text.") {
                mapOf(
                    ChannelType.GuildText to "テキストチャンネル",
                    ChannelType.GuildNews to "ニュースチャンネル",
                ).forEach { (channelType, channelTypeText) ->
                    every { VCSpeaker.kord } returns mockk {
                        every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                        coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns mockk {
                            coEvery { getChannelOrNull(Snowflake(876543210987654321)) } returns mockk<ThreadChannel> {
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
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val text =
                        "test https://discord.com/channels/123456789012345678/876543210987654321/123789456012345678"
                    val expected =
                        "test ${channelTypeText}「test-thread-parent-channel」のスレッド「test-thread-channel」で送信したメッセージのリンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
                }
            }

            // 未知のチャンネルメッセージURLを置き換える
            test("URL(s) to another message(s) on unknown channel should be replaced with readable text.") {
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                    coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns null
                }

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://discord.com/channels/123456789012345678/876543210987654321/123789456012345678"
                val expected = "test どこかのチャンネルで送信したメッセージのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                            coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns mockk {
                                coEvery { getChannelOrNull(Snowflake(876543210987654321)) } returns mockk {
                                    every { name } returns "test-channel" // テスト用のチャンネル名
                                    every { type } returns channelType
                                }
                            }
                        }

                        val message = mockk<Message>()
                        coEvery { message.getGuild() } returns mockk {
                            every { id } returns Snowflake(123456789012345678)
                        }
                        val voice = Voice(speaker = Speaker.Hikari)

                        val expected = "test ${channelTypeText}「test-channel」へのリンク"

                        val (processedText, processedVoice) = ReplacerProcessor().process(
                            message, text, voice
                        )

                        processedText shouldBe expected
                        processedVoice shouldBe voice
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
                        coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns mockk {
                            coEvery { getChannelOrNull(Snowflake(876543210987654321)) } returns mockk<ThreadChannel> {
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
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val text = "test https://discord.com/channels/123456789012345678/876543210987654321"
                    val expected =
                        "test ${channelTypeText}「test-thread-parent-channel」のスレッド「test-thread-channel」へのリンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
                }
            }

            // 未知のチャンネルURLを置き換える
            test("URL(s) to unknown channel(s) should be replaced with readable text.") {
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                    coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns null
                }

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://discord.com/channels/123456789012345678/876543210987654321"
                val expected = "test どこかのチャンネルへのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                        coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns mockk {
                            every { id } returns Snowflake(123456789012345678)
                            every { scheduledEvents } returns flow {
                                emit(mockk<GuildScheduledEvent> {
                                    every { id } returns Snowflake(876543210987654321)
                                    every { name } returns "test-event" // テスト用のイベント名
                                })
                            }
                        }
                    }

                    val message = mockk<Message>()
                    coEvery { message.getGuild() } returns mockk {
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val expected = "test イベント「test-event」へのリンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
                }
            }

            // 他のサーバでのイベントへのリンクを置き換える
            test("URL(s) to Event on external guild(s) should be replaced with readable text.") {
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                    coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns mockk {
                        every { id } returns Snowflake(123456789012345678)
                        every { name } returns "test-guild" // テスト用のサーバ名
                        every { scheduledEvents } returns flow {
                            emit(mockk<GuildScheduledEvent> {
                                every { id } returns Snowflake(876543210987654321)
                                every { name } returns "test-event" // テスト用のイベント名
                            })
                        }
                    }
                }

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123789456012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "https://discord.com/events/123456789012345678/876543210987654321"
                val expected = "サーバ「test-guild」のイベント「test-event」へのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
            }

            // 既知のサーバだが未知のイベントへのリンクを置き換える
            test("URL(s) to unknown Event on known guild(s) should be replaced with readable text.") {
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                    coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns mockk {
                        every { id } returns Snowflake(123456789012345678)
                        every { name } returns "test-guild" // テスト用のサーバ名
                        every { scheduledEvents } returns flow {}
                    }
                }

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123789456012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "https://discord.com/events/123456789012345678/123456789012345678"
                val expected = "サーバ「test-guild」のイベントへのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
            }

            // 未知のサーバでのイベントへのリンクを置き換える
            test("URL(s) to Event on unknown guild(s) should be replaced with readable text.") {
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                    coEvery { getGuildOrNull(Snowflake(123456789012345678)) } returns null
                }

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "https://discord.com/events/123456789012345678/876543210987654321"
                val expected = "どこかのサーバのイベントへのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                    coEvery { UrlReplacer["getInvite"]("abcdef", Snowflake(123456789012345678)) } returns DiscordInvite(
                        code = "abcdef",
                        guildId = Snowflake(123456789012345678),
                        guildName = "test-guild",
                        channelId = Snowflake(876543210987654321),
                        channelName = "test-channel",
                        inviterId = Snowflake(123456789012345678),
                        inviterName = "test-user",
                        eventId = Snowflake(876543210987654321),
                        eventName = "test-event",
                    )

                    val message = mockk<Message>()
                    coEvery { message.getGuild() } returns mockk {
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val expected = "test イベント「test-event」へのリンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
                }
            }

            // 他のサーバでのイベントへのリンクを置き換える
            test("If events are on another guilds.") {
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                }


                mockkObject(UrlReplacer)
                coEvery { UrlReplacer["getInvite"]("abcdef", Snowflake(123456789012345678)) } returns DiscordInvite(
                    code = "abcdef",
                    guildId = Snowflake(123456789012345678),
                    guildName = "test-guild",
                    channelId = Snowflake(876543210987654321),
                    channelName = "test-channel",
                    inviterId = Snowflake(123456789012345678),
                    inviterName = "test-user",
                    eventId = Snowflake(876543210987654321),
                    eventName = "test-event",
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123789456012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "https://discord.com/invite/abcdef?event=123456789012345678"
                val expected = "サーバ「test-guild」のイベント「test-event」へのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
            }

            // 招待リンクが取得できなかった場合の置き換え
            test("If invitation details could not be retrieved, replace it as unknown invite.") {
                every { VCSpeaker.kord } returns mockk {
                    every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
                }

                mockkObject(UrlReplacer)
                coEvery { UrlReplacer["getInvite"]("abcdef", Snowflake(123456789012345678)) } returns null

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123789456012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "https://discord.com/invite/abcdef?event=123456789012345678"
                val expected = "どこかのサーバのイベントへのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val expected = "test test-user のツイート「test-readtext」へのリンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
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
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://twitter.com/username/status/123456789012345678"
                val expected =
                    "test test-user のツイート「longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglo 以下略」へのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
            }

            // 存在するツイートで、特殊文字を含む長いツイートの場合
            test("If the tweet exists but too long to read, read first 70 characters.with special characters.") {
                mockkObject(Twitter)
                coEvery { Twitter.getTweet("username", "123456789012345678") } returns Tweet(
                    authorName = "test-user ⚠️",
                    html = "<p>test-tweet</p>",
                    plainText = "𝚐".repeat(100), // 普通の g ではない
                    readText = "𝚐".repeat(100),
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://twitter.com/username/status/123456789012345678"
                val expected =
                    "test test-user のツイート「\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90\uD835\uDE90 以下略」へのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
            }

            // 存在しないツイートの場合
            test("If the tweet not found, read it as unknown tweet.") {
                mockkObject(Twitter)
                coEvery { Twitter.getTweet("username", "123456789012345678") } returns null

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://twitter.com/username/status/123456789012345678"
                val expected = "test ユーザー「username」のツイートへのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                        guildId = Snowflake(123456789012345678),
                        guildName = "test-guild",
                        channelId = Snowflake(876543210987654321),
                        channelName = "test-channel",
                        inviterId = Snowflake(123456789012345678),
                        inviterName = "test-user",
                        eventId = Snowflake(876543210987654321),
                        eventName = "test-event",
                    )

                    val message = mockk<Message>()
                    coEvery { message.getGuild() } returns mockk {
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val expected = "test チャンネル「test-channel」への招待リンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
                }
            }

            // 他のサーバでの招待リンクを置き換える
            test("If the invite is for another guild, also read the name of the guild.") {
                mockkObject(UrlReplacer)
                coEvery { UrlReplacer["getInvite"]("abcdef", any<Snowflake>()) } returns DiscordInvite(
                    code = "abcdef",
                    guildId = Snowflake(123456789012345678),
                    guildName = "test-guild",
                    channelId = Snowflake(876543210987654321),
                    channelName = "test-channel",
                    inviterId = Snowflake(123456789012345678),
                    inviterName = "test-user",
                    eventId = Snowflake(876543210987654321),
                    eventName = "test-event",
                )

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123789456012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://discord.gg/abcdef"
                val expected = "test サーバ「test-guild」のチャンネル「test-channel」への招待リンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
            }

            // 招待リンクが取得できなかった場合の置き換え
            test("If the invitation details could not be retrieved, read it as unknown invite.") {
                mockkObject(UrlReplacer)
                coEvery { UrlReplacer["getInvite"]("abcdef", any<Snowflake>()) } returns null

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://discord.com/invite/abcdef"
                val expected = "test どこかのサーバへの招待リンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val expected = "test Steamアイテム「test-app」へのリンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
                }
            }

            // 存在しないアプリの場合
            test("If the app not found, read it as unknown app.") {
                mockkObject(Steam)
                coEvery { Steam.getAppDetail("1234567890") } returns null

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://store.steampowered.com/app/1234567890"
                val expected = "test Steamアイテムへのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val expected = "test YouTubeの「test-user」による${type}「test-video」へのリンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
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
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://www.youtube.com/watch?v=abcdefg"
                val expected = "test YouTubeの「test-usertest-u 以下略」による動画「test-videotest-video 以下略」へのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                        every { id } returns Snowflake(123456789012345678)
                    }
                    val voice = Voice(speaker = Speaker.Hikari)

                    val expected = "test YouTubeの「test-user」によるプレイリスト「test-playlist」へのリンク"

                    val (processedText, processedVoice) = ReplacerProcessor().process(
                        message, text, voice
                    )

                    processedText shouldBe expected
                    processedVoice shouldBe voice
                }
            }
        }

        // Google検索URLの置き換え
        context("Make Google Search URLs readable.") {
            // 通常の検索URL
            test("If the URL contains no special characters, just read it.") {
                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://www.google.com/search?q=example"
                val expected = "test Google検索「example」へのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
            }

            // 日本語文字列の検索URL (URLエンコードされている文字列)
            test("If the URL contains Javascript, decode the URL and read it.") {
                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(123456789012345678)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://www.google.com/search?q=%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF"
                val expected = "test Google検索「こんにちは」へのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "https://example.com"
                val expected = "Webページ「Example Domain」へのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
            }

            // 単一のURLにテキストが付随
            test("If the content contains a single URL, read its title.") {
                mockkObject(UrlReplacer)
                coEvery { UrlReplacer["getPageTitle"]("https://example.com") } returns "Example Domain"

                val message = mockk<Message>()
                coEvery { message.getGuild() } returns mockk {
                    every { id } returns Snowflake(0)
                }
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "Please visit https://example.com for more information."
                val expected = "Please visit Webページ「Example Domain」へのリンク for more information."

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "https://www.iana.org/help/example-domains explains why https://example.com is reserved."
                val expected =
                    "Webページ「Example Domains」へのリンク explains why Webページ「Example Domain」へのリンク is reserved."

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
                val voice = Voice(speaker = Speaker.Hikari)

                val text = "test https://example.com/test.jpg"
                val expected = "test JPEGファイルへのリンク"

                val (processedText, processedVoice) = ReplacerProcessor().process(
                    message, text, voice
                )

                processedText shouldBe expected
                processedVoice shouldBe voice
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
            val voice = Voice(speaker = Speaker.Hikari)

            val text = "test https://example.com/test.hoge"
            val expected = "test hogeファイルへのリンク"

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, text, voice
            )

            processedText shouldBe expected
            processedVoice shouldBe voice
        }

        // 拡張子を持たないURL
        test("If the URL doesn't have extension, read its title.") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getPageTitle"]("https://example.com/test") } returns null

            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }
            val voice = Voice(speaker = Speaker.Hikari)

            val text = "test https://example.com/test"
            val expected = "test Webページのリンク"

            val (processedText, processedVoice) = ReplacerProcessor().process(
                message, text, voice
            )

            processedText shouldBe expected
            processedVoice shouldBe voice
        }
    }
})