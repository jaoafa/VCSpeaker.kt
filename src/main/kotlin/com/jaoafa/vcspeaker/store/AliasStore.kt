package com.jaoafa.vcspeaker.store

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
enum class AliasType(val displayName: String) {
    Text("文字列"), Regex("正規表現"), Emoji("絵文字")
}

@Serializable
data class AliasData(
    val guildId: Snowflake,
    val userId: Snowflake,
    val type: AliasType,
    val from: String,
    val to: String
)

object AliasStore : StoreStruct<AliasData>(
    VCSpeaker.Files.aliases.path,
    AliasData.serializer(),
    { Json.decodeFromString(this) }
) {
    fun find(guildId: Snowflake, from: String) =
        data.find { it.guildId == guildId && it.from == from }
}

