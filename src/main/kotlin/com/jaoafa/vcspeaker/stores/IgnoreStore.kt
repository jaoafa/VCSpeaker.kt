package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class IgnoreType(
    val displayName: String,
    val emoji: String
) {
    Equals("完全一致", ":asterisk:"),
    Contains("部分一致", ":record_button:")
}

@Serializable
data class IgnoreData(
    val guildId: Snowflake,
    val userId: Snowflake,
    val type: IgnoreType,
    val search: String
) {
    fun toDisplay() = "${type.displayName}「$search」<@$userId>"

    fun toDisplayWithEmoji() = "${type.emoji} ${toDisplay()}"
}

object IgnoreStore : StoreStruct<IgnoreData>(
    VCSpeaker.Files.ignores.path,
    IgnoreData.serializer(),
    { Json.decodeFromString(this) },

    version = 1,
    migrators = mapOf(
        1 to { file ->
            val list = Json.decodeFromString<List<IgnoreData>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(IgnoreData.serializer()),
                    TypedStore(1, list)
                )
            )
        }
    )
) {
    suspend fun find(guildId: Snowflake, text: String) =
        withData { data.find { it.guildId == guildId && it.search == text } }

    suspend fun filter(guildId: Snowflake?) = withData { data.filter { it.guildId == guildId } }

    suspend fun removeForGuild(guildId: Snowflake) = withData {
        data.removeIf { it.guildId == guildId }
        writeLocked()
    }
}