package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tts.Token
import com.jaoafa.vcspeaker.tts.replacers.EmojiReplacer
import com.jaoafa.vcspeaker.tts.replacers.RegexReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.io.File

class EmojiReplacerTest : FunSpec({
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

    // 絵文字エイリアスを設定した場合、正しく置き換えられる
    test("If an emoji alias match the content, the replaced text should be returned.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Emoji,
                search = "<:world:123456789012345678>",
                replace = "world"
            )
        )

        val tokens = mutableListOf(Token("Hello, <:world:123456789012345678>!"))
        val expectedTokens = mutableListOf(
            Token("Hello, "),
            Token("world", "Emoji Alias「<:world:123456789012345678>」→「world」"),
            Token("!")
        )

        val processedTokens = EmojiReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    // 絵文字エイリアスを設定していても合致しない場合、変更されない
    test("If a emoji alias does not match the content, the text should remain unchanged.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Emoji,
                search = "<:kotlin:876543210987654321>",
                replace = "Kotlin"
            )
        )

        val tokens = mutableListOf(Token("Hello, <:world:123456789012345678>!"))

        val processedTokens = RegexReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe tokens
    }
})