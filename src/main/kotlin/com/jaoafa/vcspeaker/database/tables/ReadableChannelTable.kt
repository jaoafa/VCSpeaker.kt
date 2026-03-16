package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.database.EntitySnowflakeTransformer
import com.jaoafa.vcspeaker.database.SnowflakeTransformer
import com.jaoafa.vcspeaker.database.VersionedTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object ReadableChannelTable : IntIdTable("readable_channel"), VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_readable_channel_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_readable_channel_guild").transform(EntitySnowflakeTransformer())
    val channelDid = long("channel_did")
        .transform(SnowflakeTransformer())
    val creatorDid = long("creator_did")
        .transform(SnowflakeTransformer())
    override val version = version()

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