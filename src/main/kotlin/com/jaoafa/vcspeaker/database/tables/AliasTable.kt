package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tools.DatabaseUtil.transformSnowflake
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object AliasTable : IntIdTable("alias") {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_alias_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_alias_guild")
    val creatorDid = long("creator_did")
    val type = enumerationByName<AliasType>("type", 16)
    val search = varchar("search", 255)
    val replace = varchar("replace", 255)

    init {
        uniqueIndex(guildDid, search)
    }
}

class AliasEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AliasEntity>(AliasTable)

    var guildEntity by GuildEntity referencedOn AliasTable.guildDid
    var creatorDid by AliasTable.creatorDid.transformSnowflake()
    var type by AliasTable.type
    var search by AliasTable.search
    var replace by AliasTable.replace
}