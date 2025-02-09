package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import java.io.File

data class Speech(
    val type: TrackType,
    val guild: Guild,
    val message: Message? = null,
    val contexts: List<ProviderContext>,
    val files: List<File>
) {
    fun describe(withText: Boolean = false): String {
        val optionalText = if (withText) " \"${contexts.map { it.describe() }}\"" else ""

        return when (type) {
            TrackType.System -> "the system message$optionalText"
            TrackType.User -> "the message$optionalText by @${message?.author?.username ?: "unknown_member"}"
        }
    }
}