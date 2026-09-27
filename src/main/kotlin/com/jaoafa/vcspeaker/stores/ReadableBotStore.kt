package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.ReadableBotEntity
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
@Deprecated("Use database instead")
data class ReadableBotData(
    val guildId: Snowflake,
    val userId: Snowflake,
    val addedByUserId: Snowflake
) : DBMigratableData() {
    override fun migrationTransaction() = transaction {
        ReadableBotEntity.new {
            guildEntity = GuildEntity[guildId]
            botDid = userId
            creatorDid = addedByUserId
        }
        return@transaction
    }
}

@Deprecated("Use database instead")
object ReadableBotStore : StoreStruct<ReadableBotData>(
    VCSpeaker.Files.readableBot.path,
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
    suspend fun isReadableBot(guildId: Snowflake, user: User) =
        withData { data.any { it.guildId == guildId && it.userId == user.id } }

    suspend fun add(guildId: Snowflake, user: User, addedByUserId: Snowflake) = withData {
        if (data.any { it.guildId == guildId && it.userId == user.id }) return@withData
        data.add(ReadableBotData(guildId, user.id, addedByUserId))
        writeLocked()
    }

    suspend fun remove(guildId: Snowflake, user: User) = withData {
        data.removeIf { it.guildId == guildId && it.userId == user.id }
        writeLocked()
    }

    suspend fun removeForGuild(guildId: Snowflake) = withData {
        data.removeIf { it.guildId == guildId }
        writeLocked()
    }

    suspend fun filter(guildId: Snowflake) = withData { data.filter { it.guildId == guildId } }
}
