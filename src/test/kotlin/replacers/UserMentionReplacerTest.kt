package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.UserMentionReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.ClientResources
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.io.File

class UserMentionReplacerTest : FunSpec({
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

    // 既知のユーザーメンションを置き換える
    test("Mentions of known users should be replaced with its associated name.") {
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

        val tokens = mutableListOf(TextToken("Hello, <@123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("@test-user", "Mentionable `123456789012345678` →「test-user」"),
            TextToken("!")
        )

        val processedTokens = UserMentionReplacer.replace(
            tokens, Snowflake(0)
        )

        processedTokens shouldBe expectedTokens
    }

    // 未知のユーザーメンションを置き換える
    test("Mentions of unknown users should be replaced as unknown users.") {
        every { VCSpeaker.kord } returns mockk {
            every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
            coEvery { getGuildOrNull(Snowflake(0)) } returns null
        }

        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val tokens = mutableListOf(TextToken("Hello, <@123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("@不明なユーザー", "Mentionable `123456789012345678` →「不明なユーザー」"),
            TextToken("!")
        )

        val processedTokens = UserMentionReplacer.replace(
            tokens, Snowflake(0)
        )

        processedTokens shouldBe expectedTokens
    }
})