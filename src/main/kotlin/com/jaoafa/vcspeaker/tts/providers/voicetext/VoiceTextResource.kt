package com.jaoafa.vcspeaker.tts.providers.voicetext

import io.ktor.resources.*
import kotlinx.serialization.SerialName

@Resource("/v1/tts")
class VoiceTextResource(
    val text: String,
    val speaker: Speaker,
    val emotion: Emotion? = null,
    @SerialName("emotion_level") val emotionLevel: Int? = null,
    val pitch: Int? = null,
    val speed: Int? = null,
    val volume: Int? = null
) {
    companion object {
        fun fromContext(context: VoiceTextContext) = VoiceTextResource(
            text = context.text,
            speaker = context.voice.speaker,
            emotion = context.voice.emotion,
            emotionLevel = context.voice.emotionLevel,
            pitch = context.voice.pitch,
            speed = context.voice.speed,
            volume = context.voice.volume
        )
    }
}