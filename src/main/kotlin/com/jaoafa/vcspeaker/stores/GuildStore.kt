package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GuildData(
    val guildId: Snowflake,
    var channelId: Snowflake?,
    var prefix: String?,
    var voice: Voice,
    var autoJoin: Boolean
)

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
    suspend operator fun get(guildId: Snowflake) = withData { data.find { it.guildId == guildId } }

    suspend fun getOrDefault(guildId: Snowflake) = get(guildId) ?: GuildData(
        guildId,
        null,
        null,
        Voice(speaker = Speaker.Hikari),
        false
    )

    suspend fun getTextChannels() = withData { data.filter { it.channelId != null }.map { it.channelId!! } }

    suspend fun createOrUpdate(
        guildId: Snowflake,
        channelId: Snowflake?,
        prefix: String?,
        voice: Voice,
        autoJoin: Boolean
    ): GuildData = withData {
        val index = data.indexOfFirst { it.guildId == guildId }

        val guildData = if (index != -1) {
            data[index].apply {
                this.channelId = channelId
                this.prefix = prefix
                this.voice = voice
                this.autoJoin = autoJoin
            }
        } else {
            GuildData(guildId, channelId, prefix, voice, autoJoin).also { data.add(it) }
        }

        writeLocked()

        guildData
    }
}