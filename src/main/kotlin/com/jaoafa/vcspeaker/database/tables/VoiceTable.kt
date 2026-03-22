package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.database.VersionedTable
import com.jaoafa.vcspeaker.features.*
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object VoiceTable : IntIdTable("voice"), VersionedTable {
    val speaker = enumerationByName<Speaker>("speaker", 16)
    val emotion = enumerationByName<Emotion>("emotion", 16)
        .nullable()
    val emotionLevel = integer("emotion_level")
        .default(EMOTION_LEVEL_DEFAULT).nullable()
        .check { it.between(EMOTION_LEVEL_MIN, EMOTION_LEVEL_MAX) }
    val pitch = integer("pitch")
        .default(PITCH_DEFAULT)
        .check { it.between(PITCH_MIN, PITCH_MAX) }
    val speed = integer("speed")
        .default(SPEED_DEFAULT)
        .check { it.between(SPEED_MIN, SPEED_MAX) }
    val volume = integer("volume")
        .default(VOLUME_DEFAULT)
        .check { it.between(VOLUME_MIN, VOLUME_MAX) }
    override val version = version()

    init {
        check("check_voice_emotion_consistency") {
            not(emotion.isNull() and emotionLevel.isNotNull())
        }
    }
}

class VoiceEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<VoiceEntity>(VoiceTable)

    var speaker by VoiceTable.speaker
    var emotion by VoiceTable.emotion
    var emotionLevel by VoiceTable.emotionLevel
    var pitch by VoiceTable.pitch
    var speed by VoiceTable.speed
    var volume by VoiceTable.volume
}