package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.tables.AliasEntity
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.features.AliasType
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val AliasJson = Json {
    ignoreUnknownKeys = true
}

@Serializable
@Deprecated("Use database instead")
data class AliasData(
    val guildId: Snowflake,
    val userId: Snowflake,
    val type: AliasType,
    val search: String,
    val replace: String,
    override var migrated: Boolean = false
) : DBMigratableData {
    private val searchDisplay = if (type == AliasType.Regex) " `$search` " else "「$search」"

    private fun describe() = "${type.displayName}${searchDisplay}→「$replace」<@$userId>"

    fun describeWithEmoji() = "${type.emoji} ${describe()}"

    override fun migrate() = transaction {
        AliasEntity.new {
            guildEntity = GuildEntity[guildId]
            creatorDid = userId
            type = this@AliasData.type
            search = this@AliasData.search
            replace = this@AliasData.replace
        }
        return@transaction
    }
}

@Deprecated("Use database instead")
object AliasStore : StoreStruct<AliasData>(
    VCSpeaker.Files.aliases.path,
    AliasData.serializer(),
    { AliasJson.decodeFromString(this) },

    version = 1,
    migrators = mapOf(
        1 to { file ->
            val list = AliasJson.decodeFromString<List<AliasData>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(AliasData.serializer()),
                    TypedStore(1, list)
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
