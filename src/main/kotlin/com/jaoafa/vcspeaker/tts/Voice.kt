package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tts.api.Emotion
import com.jaoafa.vcspeaker.tts.api.Speaker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Voice(
    val speaker: Speaker,
    val emotion: Emotion? = null,
    @SerialName("emotion_level") val emotionLevel: Int = 2,
    val pitch: Int = 100,
    val speed: Int = 120,
    val volume: Int = 100
) {
    fun toJson() = Json.encodeToString(serializer(), this)

    fun copyNotNull(
        speaker: Speaker? = null,
        emotion: Emotion? = null,
        emotionLevel: Int? = null,
        pitch: Int? = null,
        speed: Int? = null,
        volume: Int? = null
    ) = Voice(
        speaker = speaker ?: this.speaker,
        emotion = emotion ?: this.emotion,
        emotionLevel = emotionLevel ?: this.emotionLevel,
        pitch = pitch ?: this.pitch,
        speed = speed ?: this.speed,
        volume = volume ?: this.volume
    )
}