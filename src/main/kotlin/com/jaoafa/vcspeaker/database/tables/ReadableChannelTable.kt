package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object ReadableChannelTable : IntIdTable("readable_channel"), VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_readable_channel_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_readable_channel_guild")
    val channelDid = long("channel_did")
        .transform(SnowflakeTransformer())
    val creatorDid = long("creator_did")
        .transform(SnowflakeTransformer())
    override val version = version()

    init {
        uniqueIndex(guildDid, channelDid)
    }
}

class ReadableChannelEntity(id: EntityID<Int>) : IntEntity(id), TypedEntity<ReadableChannelRow> {
    companion object : IntEntityClass<ReadableChannelEntity>(ReadableChannelTable)

    var guildEntity by GuildEntity referencedOn ReadableChannelTable.guildDid
    var channelDid by ReadableChannelTable.channelDid
    var creatorDid by ReadableChannelTable.creatorDid
    var version by ReadableChannelTable.version

    override fun getRow() = readValues.toTyped<ReadableChannelRow>()
}

class ReadableChannelRow(resultRow: ResultRow) : TypedRow(resultRow, ReadableChannelTable) {
    val guildDid = column(ReadableChannelTable.guildDid)
    val channelDid = column(ReadableChannelTable.channelDid)
    val creatorDid = column(ReadableChannelTable.creatorDid)
    val version = column(ReadableChannelTable.version)

    override fun describe() = "<#${channelDid}> (Added by <@${creatorDid}>)"
}