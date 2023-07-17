package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.store.CacheStore
import com.jaoafa.vcspeaker.store.GuildStore
import com.jaoafa.vcspeaker.store.VoiceStore
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.kord.common.entity.Snowflake
import java.rmi.UnexpectedException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Narrator {
    suspend fun AudioPlayer.speakSelf(text: String, guildId: Snowflake) {
        // fixme
        speak(text, GuildStore[guildId]?.voice ?: Voice(speaker = Speaker.Hikari))
    }

    suspend fun AudioPlayer.speakUser(text: String, userId: Snowflake) {
        speak(text, VoiceStore.byIdOrDefault(userId))
    }

    suspend fun AudioPlayer.speak(text: String, voice: Voice) {
        val file = if (!CacheStore.exists(text)) {
            val audio = VCSpeaker.voiceText.generateSpeech(text, voice)
            CacheStore.create(text, audio)
        } else CacheStore.read(text)

        val track = suspendCoroutine {
            VCSpeaker.lavaplayer.loadItemOrdered(
                this,
                file!!.path, // already checked
                object : AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        it.resume(track)
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist?) {
                        throw UnexpectedException("This code should not be reached.")
                    }

                    override fun noMatches() {
                        return
                    }

                    override fun loadFailed(exception: FriendlyException?) {
                        return
                    }
                })
        }

        this.playTrack(track)
    }
}