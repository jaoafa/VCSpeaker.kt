package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ReadableBotData(
    val guildId: Snowflake,
    val userId: Snowflake,
    val addedByUserId: Snowflake,
)

object ReadableBotStore : StoreStruct<ReadableBotData>(
    VCSpeaker.Files.titles.path,
    ReadableBotData.serializer(),
    { Json.decodeFromString(this) },

    version = 1,
    migrators = mapOf(
        1 to { file ->
            val list = Json.decodeFromString<List<ReadableBotData>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(ReadableBotData.serializer()),
                    TypedStore(1, list)
                )
            )
        }
    )
) {
    fun isReadableBot(guildId: Snowflake, userId: Snowflake) = data.any { it.guildId == guildId && it.userId == userId }

    fun add(guildId: Snowflake, userId: Snowflake, addedByUserId: Snowflake) {
        if (isReadableBot(guildId, userId)) return
        data.add(ReadableBotData(guildId, userId, addedByUserId))
        write()
    }

    fun remove(guildId: Snowflake, userId: Snowflake) {
        data.removeIf { it.guildId == guildId && it.userId == userId }
        write()
    }

    fun filter(guildId: Snowflake?) = data.filter { it.guildId == guildId }
}