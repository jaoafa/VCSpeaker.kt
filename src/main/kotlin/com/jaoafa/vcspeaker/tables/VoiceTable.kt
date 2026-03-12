package com.jaoafa.vcspeaker.tables

import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object VoiceTable : IntIdTable("voice") {
    val speaker = varchar("speaker", 16)
        .check { it inList Speaker.entries.map(Speaker::name) }
    val emotion = varchar("emotion", 16)
        .check { it inList Emotion.entries.map(Emotion::name) }
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
        check {
            (emotion.isNotNull() and emotionLevel.isNotNull()) or (emotion.isNull() and emotionLevel.isNull())
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