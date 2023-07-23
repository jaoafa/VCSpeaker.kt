package com.jaoafa.vcspeaker.voicetext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Voice(
    val speaker: Speaker,
    val emotion: Emotion? = null,
    @SerialName("emotion_level") val emotionLevel: Int = 2,
    val pitch: Int = 100,
    val speed: Int = 100,
    val volume: Int = 100
) {
    fun toJson() = Json.encodeToString(serializer(), this)
}