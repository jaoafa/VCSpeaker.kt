package replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.actions.GuildAction.getEntity
import com.jaoafa.vcspeaker.database.tables.AliasEntity
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.GuildTable
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.features.AliasType
import com.jaoafa.vcspeaker.tts.TextToken
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

class RegexReplacerTest : FunSpec({
    beforeSpec {
        DatabaseUtil.connect(TEST_DB_MEM_URL)
        DatabaseUtil.createTables()
    }

    // テスト前にモックを初期化
    beforeEach {
        mockkObject(VCSpeaker)
        transaction {
            GuildEntity.new(id = Snowflake(0)) {
                this.speakerVoiceEntity = VoiceEntity.new { }
            }
        }
    }

    // テスト後にモックを削除
    afterEach {
        transaction {
            GuildTable.deleteAll()
        }
        clearAllMocks()
    }

    // 正規表現エイリアスを設定した場合、正しく置き換えられる
    test("If a regex alias matches the content, the replaced text should be returned.") {
        val guild = createGuildMockk(Snowflake(0))

        transaction {
            AliasEntity.new {
                guildEntity = guild.getEntity()
                creatorDid = Snowflake(0)
                type = AliasType.Regex
                search = "w.+d"
                replace = "Kotlin"
            }
        }

        val tokens = mutableListOf(TextToken("Hello, world!"))
        val expectedTokens =
            mutableListOf(TextToken("Hello, "), TextToken("Kotlin", "Regex Alias `w.+d` →「Kotlin」"), TextToken("!"))

        val processedTokens = RegexReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    // 正規表現エイリアスを設定していても合致しない場合、変更されない
    test("If a regex alias does not match the content, the text should remain unchanged.") {
        val guild = createGuildMockk(Snowflake(0))

        transaction {
            AliasEntity.new {
                guildEntity = guild.getEntity()
                creatorDid = Snowflake(0)
                type = AliasType.Regex
                search = "w.d"
                replace = "Kotlin"
            }
        }

        val tokens = mutableListOf(TextToken("Hello, world!"))

        val processedTokens = RegexReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe tokens
    }
})
