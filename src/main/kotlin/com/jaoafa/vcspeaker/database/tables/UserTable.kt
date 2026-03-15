package com.jaoafa.vcspeaker.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object UserTable : LongIdTable("vcs_user", columnName = "did") {
    val voiceId = reference(
        "voice_id", VoiceTable,
        fkName = "fk_user_voice"
    )
}

class UserEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserEntity>(UserTable)

    var voiceEntity by VoiceEntity referencedOn UserTable.voiceId
}