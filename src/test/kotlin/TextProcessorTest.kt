import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tts.TextProcessor
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.io.File

class TextProcessorTest : FunSpec({
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

    test("processText - return unchanged text") {
        val text = "Hello, world!"
        val processed = TextProcessor.processText(Snowflake(0), text)
        processed shouldBe text
    }

    test("processText - replace URL") {
        mapOf(
            "https://example.com" to
                    "Webページ「Example Domain」へのリンク",
            "Please visit https://example.com for more information." to
                    "Please visit Webページ「Example Domain」へのリンク for more information.",
            "https://www.iana.org/help/example-domains explains why https://example.com is reserved." to
                    "Webページ「Example Domains」へのリンク explains why Webページ「Example Domain」へのリンク is reserved.",
        ).forEach { (text, replaced) ->
            val processed = TextProcessor.processText(
                Snowflake(0),
                text
            )

            processed shouldBe replaced
        }
    }

    test("processText - replace then ignore") {
        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Text,
                search = "world",
                replace = "Kotlin"
            )
        )

        IgnoreStore.create(
            IgnoreData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = IgnoreType.Contains,
                text = "Kotlin"
            )
        )

        val processed = TextProcessor.processText(Snowflake(0), "Hello, world!")
        processed.shouldBeNull()
    }

    test("processText - ignore then replace") {
        IgnoreStore.create(
            IgnoreData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = IgnoreType.Contains,
                text = "world"
            )
        )

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Text,
                search = "world",
                replace = "Kotlin"
            )
        )

        val processed = TextProcessor.processText(Snowflake(0), "Hello, world!")
        processed.shouldBeNull()
    }

    test("processText - replace url then ignore") {
        IgnoreStore.create(
            IgnoreData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = IgnoreType.Contains,
                text = "Domain"
            )
        )

        val processed = TextProcessor.processText(Snowflake(0), "Please visit https://example.com for more information.")
        processed.shouldBeNull()
    }

    test("processText - ignore then replace url") {
        IgnoreStore.create(
            IgnoreData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = IgnoreType.Contains,
                text = "https://"
            )
        )

        val processed = TextProcessor.processText(Snowflake(0), "Please visit https://example.com for more information.")
        processed.shouldBeNull()
    }

    context("ignore") {
        test("processText - ignore (equals)") {
            val text = "Hello, world!"

            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = IgnoreType.Equals,
                    text = text
                )
            )

            val processed = TextProcessor.processText(Snowflake(0), text)
            processed.shouldBeNull()
        }

        test("processText - ignore (equals) - not match") {
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = IgnoreType.Equals,
                    text = "Hello, world"
                )
            )

            val text = "Hello, world!"

            val processed = TextProcessor.processText(Snowflake(0), text)
            processed shouldBe text
        }

        test("processText - ignore (contains)") {
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = IgnoreType.Contains,
                    text = "world"
                )
            )

            val processed = TextProcessor.processText(Snowflake(0), "Hello, world!")
            processed.shouldBeNull()
        }

        test("processText - ignore (contains) - not match") {
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = IgnoreType.Contains,
                    text = "worlds"
                )
            )

            val text = "Hello, world!"

            val processed = TextProcessor.processText(Snowflake(0), text)
            processed shouldBe text
        }
    }

    context("alias") {
        test("processText - alias (text)") {
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Text,
                    search = "world",
                    replace = "Kotlin"
                )
            )

            val processed = TextProcessor.processText(Snowflake(0), "Hello, world!")
            processed shouldBe "Hello, Kotlin!"
        }

        test("processText - alias (text) - not match") {
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

            val processed = TextProcessor.processText(Snowflake(0), text)
            processed shouldBe text
        }

        test("processText - alias (regex)") {
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(0),
                    type = AliasType.Regex,
                    search = "w.+d",
                    replace = "Kotlin"
                )
            )

            val processed = TextProcessor.processText(Snowflake(0), "Hello, world!")
            processed shouldBe "Hello, Kotlin!"
        }

        test("processText - alias (regex) - not match") {
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

            val processed = TextProcessor.processText(Snowflake(0), text)
            processed shouldBe text
        }

        test("processText - alias - multiple") {
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

            val processed = TextProcessor.processText(Snowflake(0), "Hello, world!")
            processed shouldBe "Bonjour, Kotlin!"
        }

        test("processText - alias - recursive") {
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
                    type = AliasType.Text,
                    search = "Bonjour, world!",
                    replace = "你好，Kotlin!"
                )
            )

            val processed = TextProcessor.processText(Snowflake(0), "Hello, world!")
            processed shouldBe "你好，Kotlin!"
        }
    }
})