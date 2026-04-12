package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
@Deprecated("Use database instead")
data class GuildData(
    val guildId: Snowflake,
    var channelId: Snowflake?,
    var prefix: String?,
    var voice: Voice,
    var autoJoin: Boolean,
    override var migrated: Boolean = false
) : DBMigratableData {
    override fun migrate() = transaction {
        GuildEntity.new(guildId) {
            channelDid = channelId
            prefix = this@GuildData.prefix
            speakerVoiceEntity = VoiceEntity.new {
                speaker = voice.speaker
                emotion = voice.emotion
                emotionLevel = voice.emotionLevel
                pitch = voice.pitch
                speed = voice.speed
                volume = voice.volume
            }
            autoJoin = this@GuildData.autoJoin
        }
        return@transaction
    }
}

@Deprecated("Use database instead")
object GuildStore : StoreStruct<GuildData>(
    VCSpeaker.Files.guilds.path,
    GuildData.serializer(),
    { Json.decodeFromString(this) },

    version = 2,
    migrators = mapOf(
        1 to { file ->
            val list = Json.decodeFromString<List<GuildDataV1>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(GuildDataV1.serializer()),
                    TypedStore(1, list)
                )
            )
        },
        2 to { file ->
            val list = Json.decodeFromString<TypedStore<GuildDataV1>>(file.readText()).list.map { it.toV2() }
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(GuildData.serializer()),
                    TypedStore(2, list)
                )
            )
        }
    )
) {
    operator fun get(guildId: Snowflake) = data.find { it.guildId == guildId }

    fun getOrDefault(guildId: Snowflake) = data.find { it.guildId == guildId } ?: GuildData(
        guildId,
        null,
        null,
        Voice(speaker = Speaker.Hikari),
        false
    )

    fun getTextChannels() = data.filter { it.channelId != null }.map { it.channelId!! }

    fun createOrUpdate(
        guildId: Snowflake,
        channelId: Snowflake?,
        prefix: String?,
        voice: Voice,
        autoJoin: Boolean
    ): GuildData {
        val guildData = data.find { it.guildId == guildId }

        if (guildData != null)
            data[data.indexOf(guildData)] = guildData.apply {
                this.channelId = channelId
                this.prefix = prefix
                this.voice = voice
                this.autoJoin = autoJoin
            }
        else data.add(GuildData(guildId, channelId, prefix, voice, autoJoin))

        write()

        return data.find { it.guildId == guildId }!! // created or updated so it must be found
    }
}
