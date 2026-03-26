package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val DEFAULT_EMOTION_LEVEL = 2

@Serializable
data class EmotionData(
    val emotion: Emotion,
    val level: Int? = DEFAULT_EMOTION_LEVEL
)

const val DEFAULT_PITCH = 100
const val DEFAULT_SPEED = 120
const val DEFAULT_VOLUME = 100

@Serializable
data class Voice(
    val speaker: Speaker,
    val emotionData: EmotionData? = null,
    val pitch: Int = DEFAULT_PITCH,
    val speed: Int = DEFAULT_SPEED,
    val volume: Int = DEFAULT_VOLUME
) {
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