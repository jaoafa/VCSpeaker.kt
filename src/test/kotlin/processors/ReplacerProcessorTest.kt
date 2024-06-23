package processors

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.models.original.discord.DiscordInvite
import com.jaoafa.vcspeaker.models.original.twitter.Tweet
import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tools.Twitter
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
        // TODO
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

        context("replaceInviteUrl") {}

        context("replaceSteamAppUrl") {}

        context("replaceYouTubeUrl") {}

        context("replaceYouTubePlaylistUrl") {}

        context("replaceGoogleSearchUrl") {}

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

        context("replaceUrl") {}
    }
})