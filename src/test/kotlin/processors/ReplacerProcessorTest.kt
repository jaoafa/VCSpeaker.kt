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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flow
import java.io.File

class ReplacerProcessorTest : FunSpec({
    beforeTest {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File("./store-test")

        val storeStruct = mockk<StoreStruct<IgnoreData>>()
        every { storeStruct.write() } returns Unit

        mockkObject(IgnoreStore)
        every { IgnoreStore.write() } returns Unit

        IgnoreStore.data.clear()

        mockkObject(AliasStore)
        every { AliasStore.write() } returns Unit

        AliasStore.data.clear()
    }

    test("return unchanged text") {
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

    context("alias") {
        test("alias (text)") {
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

        test("alias (text) - not match") {
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

        test("alias (regex)") {
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

        test("alias (regex) - not match") {
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

        test("alias - multiple") {
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

        test("alias - non recursive") {
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
    }

    context("emoji") {
        test("replaceEmoji") {
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

    context("mentions") {
        test("Replace channel mention (known)") {
            mockkObject(VCSpeaker)
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

        test("Replace channel mention (unknown)") {
            mockkObject(VCSpeaker)
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

        test("Replace role mention (known)") {
            mockkObject(VCSpeaker)
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

        test("Replace role mention (unknown)") {
            mockkObject(VCSpeaker)
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

        test("Replace user mention (known)") {
            mockkObject(VCSpeaker)
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

        test("Replace user mention (unknown)") {
            mockkObject(VCSpeaker)
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

    context("url") {
        context("replaceMessageUrl") {
            test("known normal channel message") {
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
                        mockkObject(VCSpeaker)
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

            test("known thread channel message") {
                mapOf(
                    ChannelType.GuildText to "テキストチャンネル",
                    ChannelType.GuildNews to "ニュースチャンネル",
                ).forEach { (channelType, channelTypeText) ->
                    mockkObject(VCSpeaker)
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

            test("unknown channel message") {
                mockkObject(VCSpeaker)
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

        context("replaceChannelUrl") {
            test("known normal channel") {
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
                        mockkObject(VCSpeaker)
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

            test("known thread channel") {
                mapOf(
                    ChannelType.GuildText to "テキストチャンネル",
                    ChannelType.GuildNews to "ニュースチャンネル",
                ).forEach { (channelType, channelTypeText) ->
                    mockkObject(VCSpeaker)
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

            test("unknown channel message") {
                mockkObject(VCSpeaker)
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

        context("replaceEventDirectUrl") {
            test("this guild event") {
                listOf(
                    "test https://discord.com/events/123456789012345678/876543210987654321",
                    "test https://discordapp.com/events/123456789012345678/876543210987654321",
                    "test https://discord.com/events/123456789012345678/876543210987654321?query=example",
                    "test https://discordapp.com/events/123456789012345678/876543210987654321?query=example",
                ).forEach { text ->
                    mockkObject(VCSpeaker)
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

            test("other guild event") {
                mockkObject(VCSpeaker)
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

            test("known guild but unknown event") {
                mockkObject(VCSpeaker)
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

            test("unknown guild") {
                mockkObject(VCSpeaker)
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

        context("replaceEventInviteUrl") {
            test("this guild event") {
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
                    mockkObject(VCSpeaker)
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

            test("other guild event") {
                mockkObject(VCSpeaker)
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

            test("failed to get invite") {
                mockkObject(VCSpeaker)
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

        context("replaceTweetUrl") {
            test("exists short tweet") {
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

            test("exists long tweet") {
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

            test("exists long tweet with special characters") {
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

            test("not exists tweet") {
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

        context("replaceInviteUrl") {
            test("exists this guild invite") {
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

            test("exists other guild invite") {
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

            test("not exists invite") {
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

        context("replaceSteamAppUrl") {
            test("exists app") {
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
        }

        context("replaceYouTubeUrl") {
            test("exists video") {
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

            test("exists video but long title and long author name") {
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

        context("replaceYouTubePlaylistUrl") {
            test("exists playlist") {
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

        context("replaceGoogleSearchUrl") {
            test("normal search") {
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

            test("japanese search") {
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

        context("replaceUrlToTitle") {
            test("single url") {
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

            test("single url with text") {
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

            test("multiple urls") {
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

        context("replaceUrl") {
            test("url with defined extension") {
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

        test("url with not defined extension") {
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

        test("url without extension") {
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