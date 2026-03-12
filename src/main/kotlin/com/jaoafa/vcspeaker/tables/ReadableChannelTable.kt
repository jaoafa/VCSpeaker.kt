package com.jaoafa.vcspeaker.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object ReadableChannelTable : IntIdTable("readable_channel") {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_readable_channel_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_readable_channel_guild")
    val channelDid = long("channel_did")
    val creatorDid = long("creator_did")

    init {
        uniqueIndex(guildDid, channelDid)
    }
}

class ReadableChannelEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ReadableChannelEntity>(ReadableChannelTable)

    var guildEntity by GuildEntity referencedOn ReadableChannelTable.guildDid
    var channelDid by ReadableChannelTable.channelDid
    var creatorDid by ReadableChannelTable.creatorDid
}