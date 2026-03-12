package com.jaoafa.vcspeaker.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object UserTable : LongIdTable("user", columnName = "did") {
    val voiceId = reference(
        "voice_id", VoiceTable,
        fkName = "fk_user_voice"
    )
}