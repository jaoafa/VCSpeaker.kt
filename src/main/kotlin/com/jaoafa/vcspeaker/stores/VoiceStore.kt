package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.voicetext.api.Speaker
import com.jaoafa.vcspeaker.voicetext.Voice
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class VoiceData(
    val userId: Snowflake,
    val voice: Voice
)

object VoiceStore : StoreStruct<VoiceData>(
    VCSpeaker.Files.voices.path,
    VoiceData.serializer(),
    { Json.decodeFromString(this) }
) {
    fun byId(userId: Snowflake) = data.find { it.userId == userId }?.voice

    fun byIdOrDefault(userId: Snowflake) = byId(userId) ?: Voice(speaker = Speaker.Hikari)

    operator fun set(userId: Snowflake, voice: Voice) {
        data.removeIf { it.userId == userId }
        data.add(VoiceData(userId, voice))
        write()
    }
}