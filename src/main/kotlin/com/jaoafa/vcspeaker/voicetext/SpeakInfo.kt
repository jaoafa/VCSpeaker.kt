package com.jaoafa.vcspeaker.voicetext

import dev.kord.core.entity.Message

data class SpeakInfo(
    val text: String,
    val voice: Voice,
    val message: Message? = null
)