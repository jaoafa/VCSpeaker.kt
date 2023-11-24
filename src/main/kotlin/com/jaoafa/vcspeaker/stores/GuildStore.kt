package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
    { Json.decodeFromString(this) }
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