package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.ChannelMentionReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.ClientResources
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.io.File

class ChannelMentionReplacerTest : FunSpec({
    // テスト前にモックを初期化
    beforeTest {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker")
    }

    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // 全てのテスト後にフォルダを削除
    finalizeSpec {
        VCSpeaker.storeFolder.deleteRecursively()
    }

    // 既知のチャンネルメンションを置き換える
    test("Mentions of known channels should be replaced with their associated name.") {
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

        val tokens = mutableListOf(TextToken("Hello, <#123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("#test-channel", "Mentionable `123456789012345678` →「test-channel」"),
            TextToken("!")
        )

        val processedTokens = ChannelMentionReplacer.replace(
            tokens, Snowflake(0)
        )

        processedTokens shouldBe expectedTokens
    }

    // 未知のチャンネルメンションを置き換える
    test("Mentions of unknown channels should be replaced as unknown channels.") {
        every { VCSpeaker.kord } returns mockk {
            every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
            coEvery { getChannel(Snowflake(123456789012345678)) } returns null
        }

        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val tokens = mutableListOf(TextToken("Hello, <#123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("#不明なチャンネル", "Mentionable `123456789012345678` →「不明なチャンネル」"),
            TextToken("!")
        )

        val processedTokens = ChannelMentionReplacer.replace(
            tokens, Snowflake(0)
        )

        processedTokens shouldBe expectedTokens
    }
})