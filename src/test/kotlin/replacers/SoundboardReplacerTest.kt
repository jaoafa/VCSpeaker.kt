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
import com.jaoafa.vcspeaker.tts.replacers.SoundboardReplacer
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockkObject
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL
import utils.createGuildMockk

class SoundboardReplacerTest : FunSpec({
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

    test("If a soundboard alias matches the content, the replaced text should be returned.") {
        val replace = "https://cdn.discordapp.com/soundboard-sounds/123456789012345678.mp3"
        val guild = createGuildMockk(Snowflake(0))

        transaction {
            AliasEntity.new {
                guildEntity = guild.getEntity()
                creatorDid = Snowflake(0)
                type = AliasType.Soundboard
                search = "boom"
                this.replace = replace
            }
        }

        val tokens = mutableListOf(TextToken("Hello, boom!"))
        val expectedTokens = mutableListOf(
            TextToken("Hello, "),
            TextToken("<sound:0:123456789012345678>", "Soundboard Alias「boom」→「<sound:0:123456789012345678>」"),
            TextToken("!")
        )

        val processedTokens = SoundboardReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If a soundboard alias uses a raw id, the text should be normalized") {
        val guild = createGuildMockk(Snowflake(0))

        transaction {
            AliasEntity.new {
                guildEntity = guild.getEntity()
                creatorDid = Snowflake(0)
                type = AliasType.Soundboard
                search = "boom"
                replace = "123456789012345678"
            }
        }

        val tokens = mutableListOf(TextToken("boom"))
        val expectedTokens = mutableListOf(
            TextToken(""),
            TextToken("<sound:0:123456789012345678>", "Soundboard Alias「boom」→「<sound:0:123456789012345678>」"),
            TextToken("")
        )

        val processedTokens = SoundboardReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If a soundboard alias appears multiple times, all occurrences should be replaced") {
        val guild = createGuildMockk(Snowflake(0))

        transaction {
            AliasEntity.new {
                guildEntity = guild.getEntity()
                creatorDid = Snowflake(0)
                type = AliasType.Soundboard
                search = "boom"
                replace = "https://cdn.discordapp.com/soundboard-sounds/123456789012345678.mp3"
            }
        }

        val tokens = mutableListOf(TextToken("boom boom"))
        val expectedTokens = mutableListOf(
            TextToken(""),
            TextToken("<sound:0:123456789012345678>", "Soundboard Alias「boom」→「<sound:0:123456789012345678>」"),
            TextToken(" "),
            TextToken("<sound:0:123456789012345678>", "Soundboard Alias「boom」→「<sound:0:123456789012345678>」"),
            TextToken("")
        )

        val processedTokens = SoundboardReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe expectedTokens
    }

    test("If a soundboard alias does not match the content, the text should remain unchanged.") {
        val guild = createGuildMockk(Snowflake(0))

        transaction {
            AliasEntity.new {
                guildEntity = guild.getEntity()
                creatorDid = Snowflake(0)
                type = AliasType.Soundboard
                search = "boom"
                replace = "123456789012345678"
            }
        }

        val tokens = mutableListOf(TextToken("Hello, world!"))

        val processedTokens = SoundboardReplacer.replace(tokens, Snowflake(0))

        processedTokens shouldBe tokens
    }
})
