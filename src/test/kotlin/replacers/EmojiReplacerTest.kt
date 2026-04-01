package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.actions.GuildAction.fetchEntity
import com.jaoafa.vcspeaker.database.tables.AliasEntity
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.GuildTable
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.replacers.EmojiReplacer
import com.jaoafa.vcspeaker.tts.replacers.RegexReplacer
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockkObject
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL
import utils.createGuildMockk

class EmojiReplacerTest : FunSpec({
    beforeSpec {
        DatabaseUtil.init(TEST_DB_MEM_URL)
        DatabaseUtil.createTables()
    }

    // テスト前にモックを初期化
    beforeTest {
        mockkObject(VCSpeaker)
        transaction {
            GuildEntity.new(id = Snowflake(0)) {
                this.speakerVoiceEntity = VoiceEntity.new { }
            }
        }
    }

    // テスト後にモックを削除
    afterTest {
        transaction {
            GuildTable.deleteAll()
        }
        clearAllMocks()
    }

    // 絵文字エイリアスを設定した場合、正しく置き換えられる
    test("If an emoji alias match the content, the replaced text should be returned.") {
        val guild = createGuildMockk(Snowflake(0))

        transaction {
            AliasEntity.new {
                guildEntity = guild.fetchEntity()
                creatorDid = Snowflake(0)
                type = AliasType.Emoji
                search = "<:world:123456789012345678>"
                replace = "world"
            }
        }

        val tokens = mutableListOf(TextToken("Hello, <:world:123456789012345678>!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("world", "Emoji Alias「<:world:123456789012345678>」→「world」"),
            TextToken("!")
        )

        val processedTokens = EmojiReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    // 絵文字エイリアスを設定していても合致しない場合、変更されない
    test("If a emoji alias does not match the content, the text should remain unchanged.") {
        val guild = createGuildMockk(Snowflake(0))

        transaction {
            AliasEntity.new {
                guildEntity = guild.fetchEntity()
                creatorDid = Snowflake(0)
                type = AliasType.Emoji
                search = "<:kotlin:876543210987654321>"
                replace = "Kotlin"
            }
        }

        val tokens = mutableListOf(TextToken("Hello, <:world:123456789012345678>!"))

        val processedTokens = RegexReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe tokens
    }
})
