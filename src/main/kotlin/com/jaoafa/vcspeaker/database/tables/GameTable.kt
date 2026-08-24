package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.database.EntitySnapshot
import com.jaoafa.vcspeaker.database.SnappableEntity
import com.jaoafa.vcspeaker.database.SnapshotFactory
import com.jaoafa.vcspeaker.database.SnowflakeEntity
import com.jaoafa.vcspeaker.database.SnowflakeEntityClass
import com.jaoafa.vcspeaker.database.SnowflakeIdTable
import com.jaoafa.vcspeaker.database.VersionedTable
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object GameTable : SnowflakeIdTable("game", columnName = "did"), VersionedTable {
    val name = varchar("name", 256)
    override val version = version()
}

class GameEntity(id: EntityID<Snowflake>) : SnowflakeEntity(id), SnappableEntity<GameSnapshot, GameEntity> {
    companion object : SnowflakeEntityClass<GameEntity>(GameTable)

    var name by GameTable.name
    var version by GameTable.version

    override fun getSnapshot() = transaction { GameSnapshot.from(readValues) }
}

@Serializable
data class GameSnapshot(
    val did: Snowflake,
    val name: String,
    val version: Int,
) : EntitySnapshot<GameEntity>() {
    companion object : SnapshotFactory<GameSnapshot> {
        override fun from(row: ResultRow) = GameSnapshot(
            did = row[GameTable.id].value,
            name = row[GameTable.name],
            version = row[GameTable.version],
        )
    }

    override fun getEntity() = transaction {
        GameEntity[this@GameSnapshot.did]
    }
}
