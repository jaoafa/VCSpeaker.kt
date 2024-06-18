package processors

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.ReplacerProcessor
import dev.kord.common.entity.Snowflake
import dev.kord.core.ClientResources
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
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
            mockkStatic(VCSpeaker::class)
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
                message,
                "Hello, <#123456789012345678>!",
                voice
            )

            processedText shouldBe "Hello, #test-channel!"
            processedVoice shouldBe voice
        }

        test("Replace channel mention (unknown)") {
            mockkStatic(VCSpeaker::class)
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
                message,
                "Hello, <#123456789012345678>!",
                voice
            )

            processedText shouldBe "Hello, #不明なチャンネル!"
            processedVoice shouldBe voice
        }

        test("Replace role mention (known)") {
            mockkStatic(VCSpeaker::class)
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
                message,
                "Hello, <@&123456789012345678>!",
                voice
            )

            processedText shouldBe "Hello, @test-role!"
            processedVoice shouldBe voice
        }

        test("Replace role mention (unknown)") {
            mockkStatic(VCSpeaker::class)
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
                message,
                "Hello, <@&123456789012345678>!",
                voice
            )

            processedText shouldBe "Hello, @不明なロール!"
            processedVoice shouldBe voice
        }

        test("Replace user mention (known)") {
            mockkStatic(VCSpeaker::class)
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
                message,
                "Hello, <@123456789012345678>!",
                voice
            )

            processedText shouldBe "Hello, @test-user!"
            processedVoice shouldBe voice
        }

        test("Replace user mention (unknown)") {
            mockkStatic(VCSpeaker::class)
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
                message,
                "Hello, <@123456789012345678>!",
                voice
            )

            processedText shouldBe "Hello, @不明なユーザー!"
            processedVoice shouldBe voice
        }
    }

    context("url") {
        // TODO
    }
})