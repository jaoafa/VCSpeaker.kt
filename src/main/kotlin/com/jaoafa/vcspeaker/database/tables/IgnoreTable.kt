package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.stores.IgnoreType
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.database.EntitySnowflakeTransformer
import com.jaoafa.vcspeaker.database.SnowflakeTransformer
import com.jaoafa.vcspeaker.database.VersionedTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object IgnoreTable : IntIdTable("ignore"), VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_ignore_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_ignore_guild").transform(EntitySnowflakeTransformer())
    val creatorDid = long("creator_did").transform(SnowflakeTransformer())
    val type = enumerationByName<IgnoreType>("type", 16)
    val search = varchar("search", 255)
    override val version = version()
}

class IgnoreEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<IgnoreEntity>(IgnoreTable)

    var guildEntity by GuildEntity referencedOn IgnoreTable.guildDid
    var creatorDid by IgnoreTable.creatorDid
    var type by IgnoreTable.type
    var search by IgnoreTable.search
}