package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.AliasStore
import com.jaoafa.vcspeaker.stores.IgnoreData
import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.stores.StoreStruct
import com.jaoafa.vcspeaker.tts.Token
import com.jaoafa.vcspeaker.tts.replacers.GuildEmojiReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.io.File

class GuildEmojiReplacerTest : FunSpec({
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

    // サーバ絵文字が絵文字名に置き換わること
    test("If an server emoji found, the replaced text should be returned.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val tokens = mutableListOf(Token("Hello, <:world:123456789012345678>!"))
        val expectedTokens = mutableListOf(
            Token("Hello, "),
            Token("world", "Guild Emoji `<:world:123456789012345678>` →「world」"),
            Token("!")
        )

        val processedTokens = GuildEmojiReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    // サーバGIF絵文字が絵文字名に置き換わること
    test("If an server GIF emoji found, the replaced text should be returned.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val tokens = mutableListOf(Token("Hello, <a:world:123456789012345678>!"))
        val expectedTokens = mutableListOf(
            Token("Hello, "),
            Token("world", "Guild Emoji `<a:world:123456789012345678>` →「world」"),
            Token("!")
        )

        val processedTokens = GuildEmojiReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }
})