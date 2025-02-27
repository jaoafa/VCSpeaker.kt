package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.RoleMentionReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.ClientResources
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.io.File

class RoleMentionReplacerTest : FunSpec({
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

    // 既知のロールメンションを置き換える
    test("Mentions of known roles should be replaced with its associated name.") {
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

        val tokens = mutableListOf(TextToken("Hello, <@&123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("@test-role", "Mentionable `123456789012345678` →「test-role」"),
            TextToken("!")
        )

        val processedTokens = RoleMentionReplacer.replace(
            tokens, Snowflake(0)
        )

        processedTokens shouldBe expectedTokens
    }

    // 未知のロールメンションを置き換える
    test("Mentions of unknown roles should be replaced as unknown roles.") {
        every { VCSpeaker.kord } returns mockk {
            every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
            coEvery { getGuildOrNull(Snowflake(0)) } returns null
        }

        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val tokens = mutableListOf(TextToken("Hello, <@&123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("@不明なロール", "Mentionable `123456789012345678` →「不明なロール」"),
            TextToken("!")
        )

        val processedTokens = RoleMentionReplacer.replace(
            tokens, Snowflake(0)
        )

        processedTokens shouldBe expectedTokens
    }
})