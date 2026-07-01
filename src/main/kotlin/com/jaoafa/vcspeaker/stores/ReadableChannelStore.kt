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
    suspend fun isReadableChannel(guildId: Snowflake, channel: TextChannel) =
        withData { data.any { it.guildId == guildId && it.channelId == channel.id } }

    suspend fun add(guildId: Snowflake, channel: TextChannel, addedByUserId: Snowflake) {
        if (isReadableChannel(guildId, channel)) return
        withData {
            data.add(ReadableChannelData(guildId, channel.id, addedByUserId))
            writeLocked()
        }
    }

    suspend fun remove(guildId: Snowflake, channel: TextChannel) = withData {
        data.removeIf { it.guildId == guildId && it.channelId == channel.id }
        writeLocked()
    }

    suspend fun removeForGuild(guildId: Snowflake) = withData {
        data.removeIf { it.guildId == guildId }
        writeLocked()
    }

    suspend fun filter(guildId: Snowflake) = withData { data.filter { it.guildId == guildId } }
}