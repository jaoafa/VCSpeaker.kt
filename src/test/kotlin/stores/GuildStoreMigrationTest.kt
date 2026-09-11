package stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildData
import com.jaoafa.vcspeaker.stores.GuildDataV2
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.stores.StoreStruct
import com.jaoafa.vcspeaker.stores.TypedStore
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.serialization.json.Json
import java.io.File

@Suppress("UNCHECKED_CAST")
class GuildStoreMigrationTest : FunSpec({
    val v2Data = GuildDataV2(
        guildId = Snowflake(1),
        channelId = Snowflake(2),
        prefix = "!",
        voice = Voice(speaker = Speaker.Hikari),
        autoJoin = true
    )

    beforeSpec {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker")
        VCSpeaker.storeFolder.mkdirs()
        // GuildStore 初期化用の空ファイルを作成（未作成の場合のみ）
        val guildsFile = File(VCSpeaker.storeFolder, "guilds.json")
        if (!guildsFile.exists()) {
            guildsFile.writeText(
                Json.encodeToString(TypedStore.serializer(GuildData.serializer()), TypedStore(3, emptyList()))
            )
        }
    }

    test("GuildDataV2.toV3 sets soundboardVolume to 50 and preserves other fields") {
        val v3Data = v2Data.toV3()

        v3Data.soundboardVolume shouldBe 50
        v3Data.guildId shouldBe v2Data.guildId
        v3Data.channelId shouldBe v2Data.channelId
        v3Data.prefix shouldBe v2Data.prefix
        v3Data.voice shouldBe v2Data.voice
        v3Data.autoJoin shouldBe v2Data.autoJoin
    }

    test("the real v2 to v3 file migrator upgrades an existing guilds.json to soundboardVolume = 50") {
        val file = File.createTempFile("guilds-v2", ".json")
        file.deleteOnExit()

        file.writeText(
            Json.encodeToString(
                TypedStore.serializer(GuildDataV2.serializer()),
                TypedStore(2, listOf(v2Data))
            )
        )

        val migratorsField = StoreStruct::class.java.getDeclaredField("migrators")
        migratorsField.isAccessible = true
        val migrators = migratorsField.get(GuildStore) as Map<Int, (File) -> Unit>

        migrators[3]!!.invoke(file)

        val migrated = Json.decodeFromString(TypedStore.serializer(GuildData.serializer()), file.readText())

        migrated.version shouldBe 3
        migrated.list.single().soundboardVolume shouldBe 50
    }
})
