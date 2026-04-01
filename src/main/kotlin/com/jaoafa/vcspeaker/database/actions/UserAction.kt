package com.jaoafa.vcspeaker.database.actions

import com.jaoafa.vcspeaker.database.tables.UserEntity
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object UserAction {
    fun getVoiceOrDefaultOf(userId: Snowflake) = transaction {
        val userEntity = UserEntity.findById(userId) ?: return@transaction Voice(speaker = Speaker.Hikari)
        val voiceSnapshot = userEntity.voiceEntity.fetchSnapshot()
        return@transaction Voice.from(voiceSnapshot)
    }
}
