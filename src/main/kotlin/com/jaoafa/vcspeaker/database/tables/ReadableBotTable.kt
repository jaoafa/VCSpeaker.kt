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

object ReadableBotTable : IntIdTable("readable_bot"), VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_readable_bot_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_readable_bot_guild")
    val botDid = long("bot_did")
        .transform(SnowflakeTransformer())
    val creatorDid = long("creator_did")
        .transform(SnowflakeTransformer())
    override val version = version()

    init {
        uniqueIndex(guildDid, botDid)
    }
}

class ReadableBotEntity(id: EntityID<Int>) : IntEntity(id), SnappableEntity<ReadableBotSnapshot, ReadableBotEntity> {
    companion object : IntEntityClass<ReadableBotEntity>(ReadableBotTable)

    var guildEntity by GuildEntity referencedOn ReadableBotTable.guildDid
    var botDid by ReadableBotTable.botDid
    var creatorDid by ReadableBotTable.creatorDid
    var version by ReadableBotTable.version

    override fun getSnapshot() = transaction { ReadableBotSnapshot.from(readValues) }
}

@Serializable
data class ReadableBotSnapshot(
    val id: Int,
    val guildDid: Snowflake,
    val botDid: Snowflake,
    val creatorDid: Snowflake,
    val version: Int,
) : EntitySnapshot<ReadableBotEntity>() {
    companion object : SnapshotFactory<ReadableBotSnapshot> {
        override fun from(row: ResultRow) = ReadableBotSnapshot(
            id = row[ReadableBotTable.id].value,
            guildDid = row[ReadableBotTable.guildDid].value,
            botDid = row[ReadableBotTable.botDid],
            creatorDid = row[ReadableBotTable.creatorDid],
            version = row[ReadableBotTable.version],
        )
    }

    override fun describe() = "<@${botDid}> (Added by <@${creatorDid}>)"

    override fun getEntity() = transaction {
        ReadableBotEntity[this@ReadableBotSnapshot.id]
    }
}
