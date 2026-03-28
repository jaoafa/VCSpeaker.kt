package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.stores.IgnoreType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object IgnoreTable : IntIdTable("ignore"), VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_ignore_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_ignore_guild")
    val creatorDid = long("creator_did").transform(SnowflakeTransformer())
    val type = enumerationByName<IgnoreType>("type", 16)
    val search = varchar("search", 255)
    override val version = version()

    init {
        uniqueIndex(guildDid, search)
    }
}

class IgnoreEntity(id: EntityID<Int>) : IntEntity(id), TypedEntity<IgnoreRow> {
    companion object : IntEntityClass<IgnoreEntity>(IgnoreTable)

    var guildEntity by GuildEntity referencedOn IgnoreTable.guildDid
    var creatorDid by IgnoreTable.creatorDid
    var type by IgnoreTable.type
    var search by IgnoreTable.search
    var version by IgnoreTable.version

    override fun getRow() = readValues.toTyped<IgnoreRow>()
}

class IgnoreRow(resultRow: ResultRow) : TypedRow(resultRow, IgnoreTable) {
    val guildDid = column(IgnoreTable.guildDid)
    val creatorDid = column(IgnoreTable.creatorDid)
    val type = column(IgnoreTable.type)
    val search = column(IgnoreTable.search)
    val version = column(IgnoreTable.version)

    override fun describe() = "${type.displayName}「$search」<@$creatorDid>"

    fun describeWithEmoji() = "${type.emoji} ${describe()}"

    fun match(text: String) = when (type) {
        IgnoreType.Equals -> text == search
        IgnoreType.Contains -> text.contains(search)
    }
}