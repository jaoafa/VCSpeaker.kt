package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.voicetext.NarratorExtensions.speak
import com.kotlindiscord.kord.extensions.utils.addReaction
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Scheduler(
    private val player: AudioPlayer
) : AudioEventAdapter() {
    val queue = mutableListOf<SpeakInfo>()
    var now: SpeakInfo? = null

    suspend fun queue(message: Message? = null, text: String, voice: Voice) {
        val file = if (!CacheStore.exists(text, voice)) {
            val audio = try {
                VCSpeaker.voicetext.generateSpeech(text, voice)
            } catch (_: Exception) {
                message?.reply {
                    embed {
                        title = ":interrobang: Error!"

                        description = """
                            音声の生成に失敗しました。
                            「${message.content}」はよくわからない文字列ではありませんか？
                        """.trimIndent()

                        errorColor()
                    }
                }

                return
            }

            CacheStore.create(text, voice, audio)
        } else CacheStore.read(text, voice)!!

        val info = SpeakInfo(message, voice, file)

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
            val next = queue.removeFirst()
            now = next
            speak(next)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason): Unit =
        runBlocking {
            launch {
                (track.userData as SpeakInfo).message?.deleteOwnReaction(ReactionEmoji.Unicode("🔊"))
            }

            if (endReason.mayStartNext && queue.isNotEmpty()) {
                now = queue.removeFirst()
                launch { speak(now!!) }
            } else now = null
        }

    private suspend fun speak(info: SpeakInfo): Unit = runBlocking {
        launch {
            if (info.message != null) info.message.addReaction("🔊")
        }
        player.speak(info)
    }
}