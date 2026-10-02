package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.tts.EmotionData
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceV1(
    val speaker: Speaker,
    val emotion: Emotion? = null,
    @SerialName("emotion_level") val emotionLevel: Int = 2,
    val pitch: Int = 100,
    val speed: Int = 120,
    val volume: Int = 100
) {
    fun toV2() = Voice(
        speaker = speaker,
        emotionData = emotion?.let {
            EmotionData(
                emotion = it,
                level = emotionLevel
            )
        },
        pitch = pitch,
        speed = speed,
        volume = volume
    )
}

@Serializable
data class GuildDataV1(
    val guildId: Snowflake,
    var channelId: Snowflake?,
    var prefix: String?,
    var voice: VoiceV1,
    var autoJoin: Boolean
) {
    fun toV2() = GuildData(
        guildId = guildId,
        channelId = channelId,
        prefix = prefix,
        voice = voice.toV2(),
        autoJoin = autoJoin
    )
}

@Serializable
data class VoiceDataV1(
    val userId: Snowflake,
    val voice: VoiceV1
) {
    fun toV2() = VoiceData(
        userId = userId,
        voice = voice.toV2()
    )
}
