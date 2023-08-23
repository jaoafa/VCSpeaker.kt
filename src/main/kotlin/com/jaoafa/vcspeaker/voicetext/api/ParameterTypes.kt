package com.jaoafa.vcspeaker.voicetext.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Format {
    @SerialName("mp3") MP3,
    @SerialName("ogg") OGG,
    @SerialName("wav") WAV
}

@Serializable
enum class Speaker(val speakerName: String) {
    @SerialName("show") Show("ショウ"),
    @SerialName("haruka") Haruka("ハルカ"),
    @SerialName("hikari") Hikari("ヒカリ"),
    @SerialName("takeru") Takeru("タケル"),
    @SerialName("santa") Santa("サンタ"),
    @SerialName("bear") Bear("ベアー")
}

@Serializable
enum class Emotion(val emotionName: String) {
    @SerialName("happiness") Happiness("喜び"),
    @SerialName("anger") Anger("怒り"),
    @SerialName("sadness") Sadness("悲しみ")
}