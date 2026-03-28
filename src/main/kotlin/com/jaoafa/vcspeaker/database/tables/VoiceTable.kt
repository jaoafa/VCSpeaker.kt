package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import com.jaoafa.vcspeaker.database.TypedEntity
import com.jaoafa.vcspeaker.database.TypedRow
import com.jaoafa.vcspeaker.database.VersionedTable
import com.jaoafa.vcspeaker.database.toTyped
import com.jaoafa.vcspeaker.features.*
import com.jaoafa.vcspeaker.tools.discord.VoiceOptions
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object VoiceTable : IntIdTable("voice"), VersionedTable {
    val speaker = enumerationByName<Speaker>("speaker", 16)
        .default(Speaker.Haruka)
    val emotion = enumerationByName<Emotion>("emotion", 16)
        .nullable().default(null)
    val emotionLevel = integer("emotion_level")
        .nullable().default(null)
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

class VoiceEntity(id: EntityID<Int>) : IntEntity(id), TypedEntity<VoiceRow> {
    companion object : IntEntityClass<VoiceEntity>(VoiceTable)

    var speaker by VoiceTable.speaker
    var emotion by VoiceTable.emotion
    var emotionLevel by VoiceTable.emotionLevel
    var pitch by VoiceTable.pitch
    var speed by VoiceTable.speed
    var volume by VoiceTable.volume

    override fun getRow() = readValues.toTyped<VoiceRow>()

    fun modifyByOptions(options: VoiceOptions): Boolean {
        var modified = false

        options.pitch?.also { pitch = it; modified = true }
        options.speed?.also { speed = it; modified = true }
        options.volume?.also { volume = it; modified = true }

        // if emotion is set to be null, also set emotion level to null and stop modification lambda
        if (options.emotion == "none") {
            emotion = null
            emotionLevel = null
            return true
        }

        options.emotion?.also { emotion = Emotion.valueOf(it); modified = true }

        options.emotionLevel?.takeIf { emotion != null }?.also {
            emotionLevel = it
            modified = true
        }

        return modified
    }
}

class VoiceRow(val resultRow: ResultRow) : TypedRow(resultRow, VoiceTable) {
    val speaker = column(VoiceTable.speaker)
    val emotion = column(VoiceTable.emotion)
    val emotionLevel = column(VoiceTable.emotionLevel)
    val pitch = column(VoiceTable.pitch)
    val speed = column(VoiceTable.speed)
    val volume = column(VoiceTable.volume)

    override fun describe() = resultRow.toString()
}