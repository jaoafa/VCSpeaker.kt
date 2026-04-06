package database

import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.DatabaseUtil.getSnapshots
import com.jaoafa.vcspeaker.database.tables.*
import com.jaoafa.vcspeaker.stores.GuildData
import com.jaoafa.vcspeaker.stores.StoreStruct
import com.jaoafa.vcspeaker.tts.EmotionData
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL
import utils.ResourceUtil

@Suppress("DEPRECATION")
class StoreMigrationTest : FunSpec({
    afterEach {
        transaction {
            GuildTable.deleteAll()
            VoiceTable.deleteAll()
        }
    }

    test("Valid GuildStore should be migrated to database") {
        val tempFile = tempfile()
        ResourceUtil.loadResourceFile("/guild-test-valid.json").copyTo(tempFile, true)

        val testGuildStore = object : StoreStruct<GuildData>(
            tempFile.path,
            GuildData.serializer(),
            { Json.decodeFromString(this) }
        ) {}

        DatabaseUtil.init(TEST_DB_MEM_URL)
        transaction {
            DatabaseUtil.createTables()
        }

        println(testGuildStore.data)

        testGuildStore.migrateToDB()

        val storeData = testGuildStore.data

        storeData shouldBe mutableListOf(
            GuildData(
                guildId = Snowflake(1111111111111111111),
                channelId = Snowflake(2222222222222222222),
                prefix = null,
                voice = Voice(
                    speaker = Speaker.Hikari, emotionData = EmotionData(
                        emotion = Emotion.Happiness,
                        level = 1
                    ), pitch = 100, speed = 120, volume = 100
                ),
                autoJoin = true,
                migrated = true
            )
        )

        val dbGuildSnapshots = transaction {
            GuildEntity.all().getSnapshots()
        }
        val dbVoiceSnapshots = transaction {
            VoiceEntity.all().getSnapshots()
        }

        val speakerVoiceId = dbGuildSnapshots[0].speakerVoiceId

        dbGuildSnapshots shouldBe listOf(
            GuildSnapshot(
                did = Snowflake(1111111111111111111),
                channelDid = Snowflake(2222222222222222222),
                prefix = null,
                autoJoin = true,
                speakerVoiceId = speakerVoiceId,
                version = 0
            )
        )

        dbVoiceSnapshots shouldBe listOf(
            VoiceSnapshot(
                id = speakerVoiceId,
                speaker = Speaker.Hikari,
                emotion = Emotion.Happiness,
                emotionLevel = 1,
                pitch = 100,
                speed = 120,
                volume = 100,
                version = 0
            )
        )
    }

    test("If duplicated GuildData exist, only the first one should be migrated to database.") {
        val tempFile = tempfile()
        ResourceUtil.loadResourceFile("/guild-test-duplicate.json").copyTo(tempFile, true)

        val testGuildStore = object : StoreStruct<GuildData>(
            tempFile.path,
            GuildData.serializer(),
            { Json.decodeFromString(this) }
        ) {}

        DatabaseUtil.init(TEST_DB_MEM_URL)
        transaction {
            DatabaseUtil.createTables()
        }

        testGuildStore.migrateToDB()

        val storeData = testGuildStore.data

        storeData shouldBe mutableListOf(
            GuildData(
                guildId = Snowflake(1111111111111111111),
                channelId = Snowflake(2222222222222222222),
                prefix = null,
                voice = Voice(
                    speaker = Speaker.Hikari, emotionData = EmotionData(
                        emotion = Emotion.Happiness,
                        level = 1
                    ), pitch = 100, speed = 120, volume = 100
                ),
                autoJoin = true,
                migrated = true
            ),
            GuildData(
                guildId = Snowflake(1111111111111111111),
                channelId = Snowflake(3333333333333333333),
                prefix = "!",
                voice = Voice(
                    speaker = Speaker.Hikari,
                    emotionData = EmotionData(emotion = Emotion.Happiness, level = 2),
                    pitch = 100,
                    speed = 120,
                    volume = 100
                ),
                autoJoin = false,
                migrated = false
            )
        )

        val dbGuildSnapshots = transaction {
            GuildEntity.all().getSnapshots()
        }
        val dbVoiceSnapshots = transaction {
            VoiceEntity.all().getSnapshots()
        }

        val speakerVoiceId = dbGuildSnapshots[0].speakerVoiceId

        dbGuildSnapshots shouldBe listOf(
            GuildSnapshot(
                did = Snowflake(1111111111111111111),
                channelDid = Snowflake(2222222222222222222),
                prefix = null,
                autoJoin = true,
                speakerVoiceId = speakerVoiceId,
                version = 0
            )
        )

        dbVoiceSnapshots shouldBe listOf(
            VoiceSnapshot(
                id = speakerVoiceId,
                speaker = Speaker.Hikari,
                emotion = Emotion.Happiness,
                emotionLevel = 1,
                pitch = 100,
                speed = 120,
                volume = 100,
                version = 0
            )
        )
    }
})
