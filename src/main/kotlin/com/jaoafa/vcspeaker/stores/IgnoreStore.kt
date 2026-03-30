package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.IgnoreEntity
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// todo: move to features/
@Serializable
enum class IgnoreType(
    val displayName: String,
    val emoji: String
) {
    Equals("完全一致", ":asterisk:"),
    Contains("部分一致", ":record_button:")
}

@Serializable
@Deprecated("Use database instead")
data class IgnoreData(
    val guildId: Snowflake,
    val userId: Snowflake,
    val type: IgnoreType,
    val search: String,
    override var migrated: Boolean = false
) : DBMigratableData {
    fun toDisplay() = "${type.displayName}「$search」<@$userId>"

    fun toDisplayWithEmoji() = "${type.emoji} ${toDisplay()}"

    override fun migrate() = transaction {
        IgnoreEntity.new {
            guildEntity = GuildEntity[guildId]
            creatorDid = userId
            type = this@IgnoreData.type
            search = this@IgnoreData.search
        }
        return@transaction
    }
}

@Deprecated("Use database instead")
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
    fun find(guildId: Snowflake, text: String) = data.find { it.guildId == guildId && it.search == text }

    fun filter(guildId: Snowflake?) = data.filter { it.guildId == guildId }

    fun removeForGuild(guildId: Snowflake) {
        data.removeIf { it.guildId == guildId }
        write()
    }
}
