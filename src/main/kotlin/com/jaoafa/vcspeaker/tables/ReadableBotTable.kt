package com.jaoafa.vcspeaker.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object ReadableBotTable : IntIdTable("readable_bot") {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_readable_bot_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_readable_bot_guild")
    val botDid = long("bot_did")
    val creatorDid = long("creator_did")

    init {
        uniqueIndex(guildDid, botDid)
    }
}

class ReadableBotEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ReadableBotEntity>(ReadableBotTable)

    var guildEntity by GuildEntity referencedOn ReadableBotTable.guildDid
    var botDid by ReadableBotTable.botDid
    var creatorDid by ReadableBotTable.creatorDid
}