package processors

import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.GuildTable
import com.jaoafa.vcspeaker.database.tables.IgnoreEntity
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.features.IgnoreType
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.processors.IgnoreAfterReplaceProcessor
import com.jaoafa.vcspeaker.tts.processors.IgnoreBeforeReplaceProcessor
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL
import utils.createMessageMockk

/**
 * IgnoreProcessorのテスト
 */
class IgnoreProcessorTest : FunSpec({
    beforeSpec {
        DatabaseUtil.init(TEST_DB_MEM_URL)
        DatabaseUtil.createTables()

        transaction {
            val guildEntity = GuildEntity.new(id = Snowflake(0)) {
                this.speakerVoiceEntity = VoiceEntity.new { }
            }
            IgnoreEntity.new {
                this.guildEntity = guildEntity
                creatorDid = Snowflake(1)
                type = IgnoreType.Equals
                search = "equals"
            }
            IgnoreEntity.new {
                this.guildEntity = guildEntity
                creatorDid = Snowflake(1)
                type = IgnoreType.Contains
                search = "contains"
            }
        }
    }

    // テスト後にモックを削除
    afterEach {
        clearAllMocks()
    }

    afterSpec {
        transaction {
            GuildTable.deleteAll()
        }
    }

    context("IgnoreBeforeReplaceProcessor") {
        // 完全に一致する場合はキャンセルされる
        test("If the text exactly matches to Ignore entry, the process should be cancelled.") {
            val message = createMessageMockk(Snowflake(0))

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreBeforeReplaceProcessor()
            processor.process(message, "equals", voice)

            processor.isCancelled() shouldBe true
        }

        // 部分一致がある場合はキャンセルされる
        test("If the text partially matches to Ignore entry, the process should be cancelled.") {
            val message = createMessageMockk(Snowflake(0))

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreBeforeReplaceProcessor()
            processor.process(message, "the text contains the word contains", voice)

            processor.isCancelled() shouldBe true
        }

        // 一致するものがない場合はキャンセルされない
        test("If did not find any Ignore entries matches to the text, the process should not be cancelled.") {
            val message = createMessageMockk(Snowflake(0))

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreBeforeReplaceProcessor()
            processor.process(message, "no match", voice)

            processor.isCancelled() shouldBe false
        }
    }

    context("IgnoreAfterReplaceProcessor") {
        // 完全に一致する場合はキャンセルされる
        test("If the text exactly matches to Ignore entry, the process should be cancelled.") {
            val message = createMessageMockk(Snowflake(0))

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreAfterReplaceProcessor()
            processor.process(message, "equals", voice)

            processor.isCancelled() shouldBe true
        }

        // 部分一致がある場合はキャンセルされる
        test("If the text partially matches to Ignore entry, the process should be cancelled.") {
            val message = createMessageMockk(Snowflake(0))

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreAfterReplaceProcessor()
            processor.process(message, "the text contains the word contains", voice)

            processor.isCancelled() shouldBe true
        }

        // 一致するものがない場合はキャンセルされない
        test("If did not find any Ignore entries matches to the text, the process should not be cancelled.") {
            val message = createMessageMockk(Snowflake(0))

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreAfterReplaceProcessor()
            processor.process(message, "no match", voice)

            processor.isCancelled() shouldBe false
        }
    }
})
