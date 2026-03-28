package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.database.SnowflakeEntity
import com.jaoafa.vcspeaker.database.SnowflakeEntityClass
import com.jaoafa.vcspeaker.database.SnowflakeIdTable
import com.jaoafa.vcspeaker.database.VersionedTable
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object UserTable : SnowflakeIdTable("vcs_user", columnName = "did"), VersionedTable {
    val voiceId = reference(
        "voice_id", VoiceTable,
        fkName = "fk_user_voice"
    )
    override val version = version()
}

class UserEntity(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
    companion object : SnowflakeEntityClass<UserEntity>(UserTable)

    var voiceEntity by VoiceEntity referencedOn UserTable.voiceId
}