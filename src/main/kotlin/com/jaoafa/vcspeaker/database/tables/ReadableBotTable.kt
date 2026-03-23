package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

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

class ReadableBotEntity(id: EntityID<Int>) : IntEntity(id), TypedEntity<ReadableBotRow> {
    companion object : IntEntityClass<ReadableBotEntity>(ReadableBotTable)

    var guildEntity by GuildEntity referencedOn ReadableBotTable.guildDid
    var botDid by ReadableBotTable.botDid
    var creatorDid by ReadableBotTable.creatorDid
    var version by ReadableBotTable.version

    override fun getRow() = readValues.toTyped<ReadableBotRow>()
}

class ReadableBotRow(resultRow: ResultRow) : TypedRow(resultRow, ReadableBotTable) {
    val guildDid = column(ReadableBotTable.guildDid)
    val botDid = column(ReadableBotTable.botDid)
    val creatorDid = column(ReadableBotTable.creatorDid)
    val version = column(ReadableBotTable.version)

    override fun describe() = "<@${botDid}> (Added by <@${creatorDid}>)"
}