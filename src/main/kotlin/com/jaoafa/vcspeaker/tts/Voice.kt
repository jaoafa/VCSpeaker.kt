package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.database.tables.VoiceRow
import com.jaoafa.vcspeaker.features.EMOTION_LEVEL_DEFAULT
import com.jaoafa.vcspeaker.features.PITCH_DEFAULT
import com.jaoafa.vcspeaker.features.SPEED_DEFAULT
import com.jaoafa.vcspeaker.features.VOLUME_DEFAULT
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EmotionData(
    val emotion: Emotion,
    val level: Int? = EMOTION_LEVEL_DEFAULT
)


@Serializable
data class Voice(
    val speaker: Speaker,
    val emotionData: EmotionData? = null,
    val pitch: Int = PITCH_DEFAULT,
    val speed: Int = SPEED_DEFAULT,
    val volume: Int = VOLUME_DEFAULT
) {
    companion object {
        fun from(row: VoiceRow) = Voice(
            speaker = row.speaker,
            emotionData = row.emotion?.let { EmotionData(it, row.emotionLevel) },
            pitch = row.pitch,
            speed = row.speed,
            volume = row.volume
        )
    }

    val emotion: Emotion?
        get() = emotionData?.emotion

    val emotionLevel: Int?
        get() = emotionData?.level

    fun toJson() = Json.encodeToString(serializer(), this)

    fun copyNotNull(
        speaker: Speaker? = null,
        emotionData: EmotionData? = null,
        pitch: Int? = null,
        speed: Int? = null,
        volume: Int? = null
    ) = Voice(
        speaker = speaker ?: this.speaker,
        emotionData = emotionData ?: this.emotionData,
        pitch = pitch ?: this.pitch,
        speed = speed ?: this.speed,
        volume = volume ?: this.volume
    )
}