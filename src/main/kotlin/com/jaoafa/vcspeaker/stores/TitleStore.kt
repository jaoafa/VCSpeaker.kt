package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.VCTitleEntity
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
@Deprecated("Use database instead")
data class TitleData(
    val guildId: Snowflake,
    val channelId: Snowflake,
    val userId: Snowflake,
    val title: String? = null,
    val original: String,
    override var migrated: Boolean = false
) : DBMigratableData {
    override fun migrate() = transaction {
        VCTitleEntity.new {
            guildEntity = GuildEntity[guildId]
            channelDid = channelId
            creatorDid = userId
            title = this@TitleData.title
            originalTitle = original
        }
        return@transaction
    }
}

@Deprecated("Use database instead")
object TitleStore : StoreStruct<TitleData>(
    VCSpeaker.Files.titles.path,
    TitleData.serializer(),
    { Json.decodeFromString(this) },

    version = 1,
    migrators = mapOf(
        1 to { file ->
            val list = Json.decodeFromString<List<TitleData>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(TitleData.serializer()),
                    TypedStore(1, list)
                )
            )
        }
    )
) {
    suspend fun find(channelId: Snowflake) = withData { data.find { it.channelId == channelId } }

    suspend fun filterGuild(guildId: Snowflake) = withData { data.filter { it.guildId == guildId } }

    suspend fun removeForGuild(guildId: Snowflake) = withData {
        data.removeIf { it.guildId == guildId }
        writeLocked()
    }
}
