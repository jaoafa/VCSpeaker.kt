package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TitleData(
    val guildId: Snowflake,
    val channelId: Snowflake,
    val userId: Snowflake,
    val title: String? = null,
    val original: String
)

object TitleStore : StoreStruct<TitleData>(
    VCSpeaker.Files.titles.path,
    TitleData.serializer(),
    { Json.decodeFromString(this) }
) {
    fun find(channelId: Snowflake) = data.find { it.channelId == channelId }

    fun filterGuild(guildId: Snowflake) = data.filter { it.guildId == guildId }

    fun removeForGuild(guildId: Snowflake) {
        data.removeIf { it.guildId == guildId }
        write()
    }
}