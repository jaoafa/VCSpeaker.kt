package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.voicetext.Narrator.speak
import com.kotlindiscord.kord.extensions.utils.addReaction
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.ReactionEmoji
import kotlinx.coroutines.runBlocking

class NarratorScheduler(
    private val guildId: Snowflake,
    private val player: AudioPlayer
) : AudioEventAdapter() {
    val queue = mutableListOf<SpeakInfo>()
    var now: SpeakInfo? = null

    suspend fun queue(info: SpeakInfo) {
        if (queue.isEmpty() && now == null) {
            now = info
            speak(info)
        } else queue.add(info)
    }

    suspend fun skip() {
        if (queue.isEmpty()) {
            now = null
            player.stopTrack()
        } else {
            now = queue.removeFirst()
            speak(now!!)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        runBlocking {
            (track.userData as SpeakInfo).message?.deleteOwnReaction(ReactionEmoji.Unicode("🔊"))
        }
        
        if (endReason.mayStartNext && queue.isNotEmpty()) {
            now = queue.removeFirst()
            runBlocking {
                speak(now!!)
            }
        } else now = null
    }

    private suspend fun speak(info: SpeakInfo) {
        if (info.message != null) info.message.addReaction("🔊")
        player.speak(info)
    }
}