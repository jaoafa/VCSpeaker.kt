package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object VoiceTable : IntIdTable("voice") {
    val speaker = enumerationByName<Speaker>("speaker", 16)
    val emotion = enumerationByName<Emotion>("emotion", 16)
        .nullable()
    val emotionLevel = integer("emotion_level")
        .default(2).nullable()
        .check { it.between(1, 4) }
    val pitch = integer("pitch")
        .default(100)
        .check { it.between(50, 200) }
    val speed = integer("speed")
        .default(120)
        .check { it.between(50, 200) }
    val volume = integer("volume")
        .default(100)
        .check { it.between(50, 200) }

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