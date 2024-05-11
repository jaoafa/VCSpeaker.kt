package com.jaoafa.vcspeaker.tts

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import java.io.File

data class SpeakInfo(
    val message: Message? = null,
    val guild: Guild,
    val text: String,
    val voice: Voice,
    val file: File,
    val type: TrackType
) {
    fun getMessageLogInfo(withText: Boolean = false): String {
        val optionalText = if (withText) "\"$text\"" else ""

        return when (type) {
            TrackType.System -> "the system message$optionalText"
            TrackType.User -> "the message$optionalText by @${message?.author?.username ?: "unknown_member"}"
        }
    }
}