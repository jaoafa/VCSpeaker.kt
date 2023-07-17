package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.voicetext.Narrator.speak
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.runBlocking

class NarratorScheduler(private val player: AudioPlayer) : AudioEventAdapter() {
    val queue = mutableListOf<SpeakInfo>()
    var now: SpeakInfo? = null

    suspend fun queue(info: SpeakInfo) {
        if (queue.isEmpty() && now == null) {
            now = info
            player.speak(info.text, info.voice)
        } else queue.add(info)
    }

    suspend fun skip() {
        if (queue.isEmpty()) {
            now = null
            player.stopTrack()
        } else {
            now = queue.removeFirst()
            player.speak(now!!.text, now!!.voice)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext && queue.isNotEmpty()) {
            now = queue.removeFirst()
            runBlocking {
                player.speak(now!!.text, now!!.voice)
            }
        } else now = null
    }
}