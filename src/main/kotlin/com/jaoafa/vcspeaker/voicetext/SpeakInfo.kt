package com.jaoafa.vcspeaker.voicetext

import dev.kord.core.entity.Message
import java.io.File

data class SpeakInfo(
    val message: Message? = null,
    val text: String,
    val voice: Voice,
    val file: File
)