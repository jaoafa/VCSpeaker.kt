package com.jaoafa.vcspeaker.voicetext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Format {
    @SerialName("mp3") MP3,
    @SerialName("ogg") OGG,
    @SerialName("wav") WAV
}

@Serializable
enum class Speaker {
    @SerialName("show") SHOW,
    @SerialName("haruka") HARUKA,
    @SerialName("hikari") HIKARI,
    @SerialName("takeru") TAKERU,
    @SerialName("santa") SANTA,
    @SerialName("bear") BEAR
}

@Serializable
enum class Emotion {
    @SerialName("happiness") HAPPINESS,
    @SerialName("anger") ANGER,
    @SerialName("sadness") SADNESS
}