package replacers

import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.GuildEmojiReplacer
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GuildEmojiReplacerTest : FunSpec({
    // サーバ絵文字が絵文字名に置き換わること
    test("If an server emoji found, the replaced text should be returned.") {
        val tokens = mutableListOf(TextToken("Hello, <:world:123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("world", "Guild Emoji `<:world:123456789012345678>` →「world」"),
            TextToken("!")
        )

        val processedTokens = GuildEmojiReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    // サーバGIF絵文字が絵文字名に置き換わること
    test("If an server GIF emoji found, the replaced text should be returned.") {
        val tokens = mutableListOf(TextToken("Hello, <a:world:123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("world", "Guild Emoji `<a:world:123456789012345678>` →「world」"),
            TextToken("!")
        )

        val processedTokens = GuildEmojiReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }
})
