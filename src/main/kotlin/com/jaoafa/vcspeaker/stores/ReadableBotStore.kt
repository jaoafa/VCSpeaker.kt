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
    val addedByUserId: Snowflake,
    override var migrated: Boolean = false
) : DBMigratableData {
    override fun migrate() = transaction {
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
    fun isReadableBot(guildId: Snowflake, user: User) = data.any { it.guildId == guildId && it.userId == user.id }

    fun add(guildId: Snowflake, user: User, addedByUserId: Snowflake) {
        if (isReadableBot(guildId, user)) return
        data.add(ReadableBotData(guildId, user.id, addedByUserId))
        write()
    }

    fun remove(guildId: Snowflake, user: User) {
        data.removeIf { it.guildId == guildId && it.userId == user.id }
        write()
    }

    fun removeForGuild(guildId: Snowflake) {
        data.removeIf { it.guildId == guildId }
        write()
    }
}
