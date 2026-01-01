package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.AliasData
import com.jaoafa.vcspeaker.stores.AliasStore
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.stores.IgnoreData
import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.stores.StoreStruct
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.SoundboardReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.io.File

class SoundboardReplacerTest : FunSpec({
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

    afterTest {
        clearAllMocks()
    }

    finalizeSpec {
        VCSpeaker.storeFolder.deleteRecursively()
    }

    test("If a soundboard alias matches the content, the replaced text should be returned.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val replace = "https://cdn.discordapp.com/soundboard-sounds/123456789012345678.mp3"

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Soundboard,
                search = "boom",
                replace = replace
            )
        )

        val tokens = mutableListOf(TextToken("Hello, boom!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("<sound:0:123456789012345678>", "Soundboard Alias「boom」→「<sound:0:123456789012345678>」"),
            TextToken("!")
        )

        val processedTokens = SoundboardReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If a soundboard alias uses a raw id, the text should be normalized") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Soundboard,
                search = "boom",
                replace = "123456789012345678"
            )
        )

        val tokens = mutableListOf(TextToken("boom"))
        val expectedTokens = mutableListOf(
            TextToken(""),
            TextToken("<sound:0:123456789012345678>", "Soundboard Alias「boom」→「<sound:0:123456789012345678>」"),
            TextToken("")
        )

        val processedTokens = SoundboardReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If a soundboard alias appears multiple times, all occurrences should be replaced") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Soundboard,
                search = "boom",
                replace = "https://cdn.discordapp.com/soundboard-sounds/123456789012345678.mp3"
            )
        )

        val tokens = mutableListOf(TextToken("boom boom"))
        val expectedTokens = mutableListOf(
            TextToken(""),
            TextToken("<sound:0:123456789012345678>", "Soundboard Alias「boom」→「<sound:0:123456789012345678>」"),
            TextToken(" "),
            TextToken("<sound:0:123456789012345678>", "Soundboard Alias「boom」→「<sound:0:123456789012345678>」"),
            TextToken("")
        )

        val processedTokens = SoundboardReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If a soundboard alias does not match the content, the text should remain unchanged.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Soundboard,
                search = "boom",
                replace = "123456789012345678"
            )
        )

        val tokens = mutableListOf(TextToken("Hello, world!"))

        val processedTokens = SoundboardReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe tokens
    }
})
