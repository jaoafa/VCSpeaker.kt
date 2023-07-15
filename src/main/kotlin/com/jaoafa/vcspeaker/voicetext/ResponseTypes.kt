package com.jaoafa.vcspeaker.voicetext

import kotlinx.serialization.Serializable

@Serializable
data class VoiceTextError(
    val error: VoiceTextErrorMessage
)

@Serializable
data class VoiceTextErrorMessage(
    val message: String
)