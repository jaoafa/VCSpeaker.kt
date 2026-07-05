package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.tables.UserEntity
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
@Deprecated("Use database instead")
data class VoiceData(
    val userId: Snowflake,
    val voice: Voice,
    override var migrated: Boolean = false
) : DBMigratableData {
    override fun migrate() = transaction {
        UserEntity.new(userId) {
            voiceEntity = VoiceEntity.new {
                speaker = voice.speaker
                emotion = voice.emotion
                emotionLevel = voice.emotionLevel
                pitch = voice.pitch
                speed = voice.speed
                volume = voice.volume
            }
        }
        return@transaction
    }
}

@Deprecated("Use database instead")
object VoiceStore : StoreStruct<VoiceData>(
    VCSpeaker.Files.voices.path,
    VoiceData.serializer(),
    { Json.decodeFromString(this) },

    version = 2,
    migrators = mapOf(
        1 to { file ->
            val list = Json.decodeFromString<List<VoiceDataV1>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(VoiceDataV1.serializer()),
                    TypedStore(1, list)
                )
            )
        },
        2 to { file ->
            val list = Json.decodeFromString<TypedStore<VoiceDataV1>>(file.readText()).list.map { it.toV2() }
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(VoiceData.serializer()),
                    TypedStore(2, list)
                )
            )
        }
    )
) {
    fun byId(userId: Snowflake) = data.find { it.userId == userId }?.voice

    fun byIdOrDefault(userId: Snowflake) = byId(userId) ?: Voice(speaker = Speaker.Hikari)

    operator fun set(userId: Snowflake, voice: Voice) {
        data.removeIf { it.userId == userId }
        data.add(VoiceData(userId, voice))
        write()
    }

    fun remove(userId: Snowflake) {
        data.removeIf { it.userId == userId }
        write()
    }
}
