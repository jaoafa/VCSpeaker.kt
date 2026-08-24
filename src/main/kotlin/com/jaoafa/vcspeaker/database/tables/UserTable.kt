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
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object UserTable : SnowflakeIdTable("vcs_user", columnName = "did"), VersionedTable {
    val voiceId = reference(
        "voice_id", VoiceTable,
        fkName = "fk_user_voice"
    )
    override val version = version()
}

class UserEntity(id: EntityID<Snowflake>) : SnowflakeEntity(id), SnappableEntity<UserSnapshot, UserEntity> {
    companion object : SnowflakeEntityClass<UserEntity>(UserTable)

    var voiceEntity by VoiceEntity referencedOn UserTable.voiceId
    var version by VoiceTable.version

    override fun getSnapshot() = transaction { UserSnapshot.from(readValues) }
}

@Serializable
data class UserSnapshot(
    val did: Snowflake,
    val voiceId: Int,
    val version: Int,
) : EntitySnapshot<UserEntity>() {
    companion object : SnapshotFactory<UserSnapshot> {
        override fun from(row: org.jetbrains.exposed.v1.core.ResultRow) = UserSnapshot(
            did = row[UserTable.id].value,
            voiceId = row[UserTable.voiceId].value,
            version = row[UserTable.version],
        )
    }

    override fun getEntity() = transaction {
        UserEntity[this@UserSnapshot.did]
    }
}
