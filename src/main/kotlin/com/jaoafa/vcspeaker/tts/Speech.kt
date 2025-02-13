package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message

data class Speech(
    val type: TrackType,
    val guild: Guild,
    val message: Message? = null,
    val contexts: List<ProviderContext>,
    val tracks: List<AudioTrack>
) {
    private var index: Int = 0

    fun next(): AudioTrack? {
        index++

        if (index >= tracks.size) return null
        return tracks[index]
    }

    fun describe(withText: Boolean = false): String {
        val optionalText = if (withText) " \"${contexts.map { it.describe() }}\"" else ""

        return when (type) {
            TrackType.System -> "the system message$optionalText"
            TrackType.User -> "the message$optionalText by @${message?.author?.username ?: "unknown_member"}"
        }
    }

    val id = Snowflake(System.currentTimeMillis())
}