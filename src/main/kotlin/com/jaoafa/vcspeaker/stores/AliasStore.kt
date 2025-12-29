package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class AliasType(
    val displayName: String,
    val emoji: String
) {
    Text("文字列", ":pencil:"),
    Regex("正規表現", ":asterisk:"),
    Emoji("絵文字", ":neutral_face:")
}

@Serializable
data class AliasData(
    val guildId: Snowflake,
    val userId: Snowflake,
    val type: AliasType,
    val search: String,
    val replace: String,
    val soundboard: Snowflake? = null
) {
    private val searchDisplay = if (type == AliasType.Regex) " `$search` " else "「$search」"

    private fun describe() = "${type.displayName}${searchDisplay}→${if (soundboard != null) "🔊 Soundboard" else "「$replace」"}<@$userId>"

    fun describeWithEmoji() = "${type.emoji} ${describe()}"
}

object AliasStore : StoreStruct<AliasData>(
    VCSpeaker.Files.aliases.path,
    AliasData.serializer(),
    { Json.decodeFromString(this) },

    version = 2,
    migrators = mapOf(
        1 to { file ->
            val list = Json.decodeFromString<List<AliasData>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(AliasData.serializer()),
                    TypedStore(1, list)
                )
            )
        },
        2 to { file ->
            // Migration for adding soundboard field
            // Since soundboard is nullable with default null, existing data will work
            val store = Json.decodeFromString<TypedStore<AliasData>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(AliasData.serializer()),
                    TypedStore(2, store.list)
                )
            )
        }
    ),
    auditor = { data ->
        data.sortedByDescending { it.search.length }.toMutableList()
    }
) {
    fun find(guildId: Snowflake, from: String) =
        data.find { it.guildId == guildId && it.search == from }

    fun filter(guildId: Snowflake?) = data.filter { it.guildId == guildId }

    fun removeForGuild(guildId: Snowflake) {
        data.removeIf { it.guildId == guildId }
        write()
    }
}

