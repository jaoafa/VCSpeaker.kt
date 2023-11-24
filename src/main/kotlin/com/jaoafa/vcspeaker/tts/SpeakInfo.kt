package com.jaoafa.vcspeaker.tts

import dev.kord.core.entity.Message
import java.io.File

data class SpeakInfo(
    val message: Message? = null,
    val voice: Voice,
    val file: File
)