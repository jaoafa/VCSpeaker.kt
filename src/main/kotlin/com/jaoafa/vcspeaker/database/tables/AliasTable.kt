package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object AliasTable : IntIdTable("alias"), DiffUpsertableTable<AliasRow>, VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_alias_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_alias_guild")
    val creatorDid = long("creator_did").transform(SnowflakeTransformer())
    val type = enumerationByName<AliasType>("type", 16)
    val search = varchar("search", 255)
    val replace = varchar("replace", 255)
    override val version = version()

    override val uniqueColumns = listOf(guildDid, search)
    override fun getConflictOp(values: Map<Column<*>, Any?>): Op<Boolean> {
        @Suppress("UNCHECKED_CAST")
        return (guildDid eq values[guildDid] as EntityID<Snowflake>) and (search eq values[search] as String)
    }

    init {
        uniqueIndex(guildDid, search)
    }
}

class AliasEntity(id: EntityID<Int>) : IntEntity(id), TypedEntity<AliasRow> {
    companion object : IntEntityClass<AliasEntity>(AliasTable)

    var guildEntity by GuildEntity referencedOn AliasTable.guildDid
    var creatorDid by AliasTable.creatorDid
    var type by AliasTable.type
    var search by AliasTable.search
    var replace by AliasTable.replace
    var version by AliasTable.version

    override fun getRow() = readValues.toTyped<AliasRow>()
}

class AliasRow(resultRow: ResultRow) : TypedRow(resultRow, AliasTable) {
    val guildDid = column(AliasTable.guildDid)
    val creatorDid = column(AliasTable.creatorDid)
    val type = column(AliasTable.type)
    val search = column(AliasTable.search)
    val replace = column(AliasTable.replace)
    val version = column(AliasTable.version)

    private val searchDisplay = if (type == AliasType.Regex) " `$search` " else "「$search」"

    override fun describe() = "${type.displayName}${searchDisplay}→「$replace」<@$creatorDid>"

    fun describeWithEmoji() = "${type.emoji} ${describe()}"
}