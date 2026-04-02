package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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

class ReadableChannelEntity(id: EntityID<Int>) : IntEntity(id),
    SnappableEntity<ReadableChannelSnapshot, ReadableChannelEntity> {
    companion object : IntEntityClass<ReadableChannelEntity>(ReadableChannelTable)

    var guildEntity by GuildEntity referencedOn ReadableChannelTable.guildDid
    var channelDid by ReadableChannelTable.channelDid
    var creatorDid by ReadableChannelTable.creatorDid
    var version by ReadableChannelTable.version

    override fun getSnapshot() = transaction { ReadableChannelSnapshot.from(readValues) }
}

@Serializable
data class ReadableChannelSnapshot(
    val id: Int,
    val guildDid: Snowflake,
    val channelDid: Snowflake,
    val creatorDid: Snowflake,
    val version: Int,
) : EntitySnapshot<ReadableChannelEntity>() {
    companion object : SnapshotFactory<ReadableChannelSnapshot> {
        override fun from(row: ResultRow) = ReadableChannelSnapshot(
            id = row[ReadableChannelTable.id].value,
            guildDid = row[ReadableChannelTable.guildDid].value,
            channelDid = row[ReadableChannelTable.channelDid],
            creatorDid = row[ReadableChannelTable.creatorDid],
            version = row[ReadableChannelTable.version],
        )
    }

    override fun describe() = "<#${channelDid}> (Added by <@${creatorDid}>)"

    override fun getEntity() = transaction {
        ReadableChannelEntity[this@ReadableChannelSnapshot.id]
    }
}
