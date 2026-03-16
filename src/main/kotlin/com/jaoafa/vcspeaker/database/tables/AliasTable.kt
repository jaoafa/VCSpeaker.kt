package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.database.DiffUpsertableTable
import com.jaoafa.vcspeaker.database.EntitySnowflakeTransformer
import com.jaoafa.vcspeaker.database.SnowflakeTransformer
import com.jaoafa.vcspeaker.database.TypedRow
import com.jaoafa.vcspeaker.database.VersionedTable
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
    ).index("idx_alias_guild").transform(EntitySnowflakeTransformer())
    val creatorDid = long("creator_did").transform(SnowflakeTransformer())
    val type = enumerationByName<AliasType>("type", 16)
    val search = varchar("search", 255)
    val replace = varchar("replace", 255)
    override val version = version()

    override val uniqueColumns = listOf(guildDid, search)
    override fun getConflictOp(values: Map<Column<*>, Any?>): Op<Boolean> {
        return (guildDid eq values[guildDid] as Snowflake) and (search eq values[search] as String)
    }

    init {
        uniqueIndex(guildDid, search)
    }
}

class AliasEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AliasEntity>(AliasTable)

    var guildEntity by GuildEntity referencedOn AliasTable.guildDid
    var creatorDid by AliasTable.creatorDid
    var type by AliasTable.type
    var search by AliasTable.search
    var replace by AliasTable.replace
    var version by AliasTable.version
}

data class AliasRow(val resultRow: ResultRow) : TypedRow(resultRow, AliasTable) {
    val guildDid by column(AliasTable.guildDid)
    val creatorDid by column(AliasTable.creatorDid)
    val type by column(AliasTable.type)
    val search by column(AliasTable.search)
    val replace by column(AliasTable.replace)
    val version by column(AliasTable.version)
}