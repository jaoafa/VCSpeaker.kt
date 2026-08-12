package replacers

import com.jaoafa.vcspeaker.tools.discord.GameResolver
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.GameMentionReplacer
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject

class GameMentionReplacerTest : FunSpec({
    afterTest {
        clearAllMocks()
    }

    test("If a known game mention is found, it should be replaced with the game name.") {
        mockkObject(GameResolver)
        coEvery { GameResolver.resolve(listOf(123456789L)) } returns mapOf(123456789L to "Test Game")

        val tokens = mutableListOf(TextToken("Playing <@$123456789> now!"))
        val expectedTokens = mutableListOf(
            TextToken("Playing "),
            TextToken("Test Game", "Game `123456789` →「Test Game」"),
            TextToken(" now!")
        )

        val processedTokens = GameMentionReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If an unresolved game mention is found, it should be replaced with 「ゲーム」.") {
        mockkObject(GameResolver)
        coEvery { GameResolver.resolve(listOf(999L)) } returns mapOf(999L to null)

        val tokens = mutableListOf(TextToken("Playing <@$999> now!"))
        val expectedTokens = mutableListOf(
            TextToken("Playing "),
            TextToken("ゲーム", "Game `999` → 不明"),
            TextToken(" now!")
        )

        val processedTokens = GameMentionReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If no game mention is found, the text should not be changed.") {
        val tokens = mutableListOf(TextToken("Hello, world!"))

        val processedTokens = GameMentionReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe tokens
    }

    test("If multiple game mentions are found, all should be replaced.") {
        mockkObject(GameResolver)
        coEvery { GameResolver.resolve(listOf(1L, 2L)) } returns mapOf(1L to "Game One", 2L to "Game Two")

        val tokens = mutableListOf(TextToken("<@$1> and <@$2>"))
        val expectedTokens = mutableListOf(
            TextToken("Game One", "Game `1` →「Game One」"),
            TextToken(" and "),
            TextToken("Game Two", "Game `2` →「Game Two」")
        )

        val processedTokens = GameMentionReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If the same game id appears multiple times, GameResolver should be queried once with distinct ids.") {
        mockkObject(GameResolver)
        coEvery { GameResolver.resolve(listOf(1L)) } returns mapOf(1L to "Game One")

        val tokens = mutableListOf(TextToken("<@$1> and <@$1> again"))

        GameMentionReplacer.replace(tokens, Snowflake(0))

        coVerify(exactly = 1) { GameResolver.resolve(listOf(1L)) }
    }
})
