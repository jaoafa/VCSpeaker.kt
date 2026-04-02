package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.features.IgnoreType
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object IgnoreTable : IntIdTable("ignore"), VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_ignore_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_ignore_guild")
    val creatorDid = long("creator_did").transform(SnowflakeTransformer())
    val type = enumerationByName<IgnoreType>("type", 16)
    val search = varchar("search", 255)
    override val version = version()

    init {
        uniqueIndex(guildDid, search)
    }
}

class IgnoreEntity(id: EntityID<Int>) : IntEntity(id), SnappableEntity<IgnoreSnapshot, IgnoreEntity> {
    companion object : IntEntityClass<IgnoreEntity>(IgnoreTable)

    var guildEntity by GuildEntity referencedOn IgnoreTable.guildDid
    var creatorDid by IgnoreTable.creatorDid
    var type by IgnoreTable.type
    var search by IgnoreTable.search
    var version by IgnoreTable.version

    override fun getSnapshot() = transaction { IgnoreSnapshot.from(readValues) }
}

@Serializable
data class IgnoreSnapshot(
    val id: Int,
    val guildDid: Snowflake,
    val creatorDid: Snowflake,
    val type: IgnoreType,
    val search: String,
    val version: Int,
) : EntitySnapshot<IgnoreEntity>() {
    companion object : SnapshotFactory<IgnoreSnapshot> {
        override fun from(row: ResultRow) = IgnoreSnapshot(
            id = row[IgnoreTable.id].value,
            guildDid = row[IgnoreTable.guildDid].value,
            creatorDid = row[IgnoreTable.creatorDid],
            type = row[IgnoreTable.type],
            search = row[IgnoreTable.search],
            version = row[IgnoreTable.version],
        )
    }

    override fun describe() = "${type.displayName}「$search」<@$creatorDid>"

    fun describeWithEmoji() = "${type.emoji} ${describe()}"

    override fun getEntity() = transaction {
        IgnoreEntity[this@IgnoreSnapshot.id]
    }

    fun match(text: String) = when (type) {
        IgnoreType.Equals -> text == search
        IgnoreType.Contains -> text.contains(search)
    }
}
