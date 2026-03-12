package com.jaoafa.vcspeaker.tables

import com.jaoafa.vcspeaker.stores.AliasType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object AliasTable : IntIdTable("alias") {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_alias_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_alias_guild")
    val creatorDid = long("creator_did")
    val type = varchar("type", 16)
        .check { it inList AliasType.entries.map(AliasType::name) }
    val search = varchar("search", 255)
    val replace = varchar("replace", 255)
}

class AliasEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AliasEntity>(AliasTable)

    var guildEntity by GuildEntity referencedOn AliasTable.guildDid
    var creatorDid by AliasTable.creatorDid
    var type by AliasTable.type
    var search by AliasTable.search
    var replace by AliasTable.replace
}