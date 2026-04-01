package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object AliasTable : IntIdTable("alias"), VersionedTable {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_alias_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_alias_guild")
    val creatorDid = long("creator_did").transform(SnowflakeTransformer())
    val type = enumerationByName<AliasType>("type", 16)
    val search = varchar("search", 255)
    val replace = varchar("replace", 255)
    override val version = version()

    init {
        uniqueIndex(guildDid, search)
    }
}

class AliasEntity(id: EntityID<Int>) : IntEntity(id), SnappableEntity<AliasSnapshot, AliasEntity> {
    companion object : IntEntityClass<AliasEntity>(AliasTable)

    var guildEntity by GuildEntity referencedOn AliasTable.guildDid
    var creatorDid by AliasTable.creatorDid
    var type by AliasTable.type
    var search by AliasTable.search
    var replace by AliasTable.replace
    var version by AliasTable.version

    override fun fetchSnapshot() = transaction { AliasSnapshot.from(readValues) }
}

@Serializable
data class AliasSnapshot(
    val id: Int,
    val guildDid: Snowflake,
    val creatorDid: Snowflake,
    val type: AliasType,
    val search: String,
    val replace: String,
    val version: Int,
) : EntitySnapshot<AliasEntity>() {
    companion object : SnapshotFactory<AliasSnapshot> {
        override fun from(row: ResultRow) = AliasSnapshot(
            id = row[AliasTable.id].value,
            guildDid = row[AliasTable.guildDid].value,
            creatorDid = row[AliasTable.creatorDid],
            type = row[AliasTable.type],
            search = row[AliasTable.search],
            replace = row[AliasTable.replace],
            version = row[AliasTable.version]
        )
    }

    override fun fetchEntity() = transaction {
        AliasEntity[this@AliasSnapshot.id]
    }

    private val searchDisplay = if (type == AliasType.Regex) " `$search` " else "「$search」"

    override fun describe() = "${type.displayName}${searchDisplay}→「$replace」<@$creatorDid>"

    fun describeWithEmoji() = "${type.emoji} ${describe()}"
}
