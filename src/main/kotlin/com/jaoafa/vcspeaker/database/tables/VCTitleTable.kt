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

object VCTitleTable : IntIdTable("vc_title"), VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_vc_title_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_vc_title_guild")
    val title = varchar("title", 255).nullable()
    val originalTitle = varchar("original_title", 255)
    val channelDid = long("channel_did")
        .uniqueIndex("idx_vc_title_channel")
        .transform(SnowflakeTransformer())
    val creatorDid = long("creator_did")
        .transform(SnowflakeTransformer())
    override val version = version()
}

class VCTitleEntity(id: EntityID<Int>) : IntEntity(id), SnappableEntity<VCTitleSnapshot, VCTitleEntity> {
    companion object : IntEntityClass<VCTitleEntity>(VCTitleTable)

    var guildEntity by GuildEntity referencedOn VCTitleTable.guildDid
    var title by VCTitleTable.title
    var originalTitle by VCTitleTable.originalTitle
    var channelDid by VCTitleTable.channelDid
    var creatorDid by VCTitleTable.creatorDid
    var version by VCTitleTable.version

    override fun getSnapshot() = transaction { VCTitleSnapshot.from(readValues) }
}

@Serializable
data class VCTitleSnapshot(
    val id: Int,
    val guildDid: Snowflake,
    val title: String?,
    val originalTitle: String,
    val channelDid: Snowflake,
    val creatorDid: Snowflake,
    val version: Int,
) : EntitySnapshot<VCTitleEntity>() {
    companion object : SnapshotFactory<VCTitleSnapshot> {
        override fun from(row: ResultRow) = VCTitleSnapshot(
            id = row[VCTitleTable.id].value,
            guildDid = row[VCTitleTable.guildDid].value,
            title = row[VCTitleTable.title],
            originalTitle = row[VCTitleTable.originalTitle],
            channelDid = row[VCTitleTable.channelDid],
            creatorDid = row[VCTitleTable.creatorDid],
            version = row[VCTitleTable.version],
        )
    }

    override fun describe() = "$title <#$channelDid> by <@$creatorDid> ($originalTitle)"

    override fun getEntity() = transaction {
        VCTitleEntity[this@VCTitleSnapshot.id]
    }
}
