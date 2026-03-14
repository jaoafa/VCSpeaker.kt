package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.stores.IgnoreType
import com.jaoafa.vcspeaker.tools.DatabaseUtil.transformSnowflake
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object IgnoreTable : IntIdTable("ignore") {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_ignore_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_ignore_guild")
    val creatorDid = long("creator_did")
    val type = enumerationByName<IgnoreType>("type", 16)
    val search = varchar("search", 255)
}

class IgnoreEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<IgnoreEntity>(IgnoreTable)

    var guildEntity by GuildEntity referencedOn IgnoreTable.guildDid
    var creatorDid by IgnoreTable.creatorDid.transformSnowflake()
    var type by IgnoreTable.type
    var search by IgnoreTable.search
}