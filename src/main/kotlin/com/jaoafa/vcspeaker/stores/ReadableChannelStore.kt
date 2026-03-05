package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.channel.TextChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ReadableChannelData(
    val guildId: Snowflake,
    val channelId: Snowflake,
    val addedByUserId: Snowflake,
)

object ReadableChannelStore : StoreStruct<ReadableChannelData>(
    VCSpeaker.Files.readableChannel.path,
    ReadableChannelData.serializer(),
    { Json.decodeFromString(this) },

    version = 1,
    migrators = mapOf(
        1 to { file ->
            val list = Json.decodeFromString<List<ReadableChannelData>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(ReadableChannelData.serializer()),
                    TypedStore(1, list)
                )
            )
        }
    )
) {
    fun isReadableChannel(guildId: Snowflake, channel: TextChannel) =
        data.any { it.guildId == guildId && it.channelId == channel.id }

    fun add(guildId: Snowflake, channel: TextChannel, addedByUserId: Snowflake) {
        if (isReadableChannel(guildId, channel)) return
        data.add(ReadableChannelData(guildId, channel.id, addedByUserId))
        write()
    }

    fun remove(guildId: Snowflake, channel: TextChannel) {
        data.removeIf { it.guildId == guildId && it.channelId == channel.id }
        write()
    }

    fun removeForGuild(guildId: Snowflake) {
        data.removeIf { it.guildId == guildId }
        write()
    }
}