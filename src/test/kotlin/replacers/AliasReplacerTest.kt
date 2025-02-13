package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.AliasReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.io.File

class AliasReplacerTest : FunSpec({
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

    // テキストエイリアスを設定した場合、正しく置き換えられる
    test("If a text alias matches the message, the replaced text should be returned.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Text,
                search = "world",
                replace = "Kotlin"
            )
        )

        val tokens = mutableListOf(TextToken("Hello, world!"))
        val expectedTokens = mutableListOf(TextToken("Hello, "), TextToken("Kotlin", "Text Alias「world」→「Kotlin」"), TextToken("!"))

        val processedTokens = AliasReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    // テキストエイリアスを設定していても合致しない場合、変更されない
    test("If a text alias does not match the content, the text should remain unchanged.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        AliasStore.create(
            AliasData(
                guildId = Snowflake(0),
                userId = Snowflake(0),
                type = AliasType.Text,
                search = "Java",
                replace = "Kotlin"
            )
        )

        val tokens = mutableListOf(TextToken("Hello, world!"))

        val processedTokens = AliasReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe tokens
    }
})