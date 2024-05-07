package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.speak
import com.kotlindiscord.kord.extensions.utils.addReaction
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.rest.builder.message.embed
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

class Scheduler(
    private val player: AudioPlayer
) : AudioEventAdapter() {
    private val logger = KotlinLogging.logger { }

    val queue = mutableListOf<SpeakInfo>()
    var now: SpeakInfo? = null

    suspend fun queue(
        message: Message? = null, text: String, voice: Voice, guild: Guild, type: TrackType
    ) {
        val guildName = guild.name

        val messageInfo = "the message by @${message?.author?.username ?: "unknown_member"}"

        val file = if (!CacheStore.exists(text, voice)) {
            val downloadTime: Long

            val audio: ByteArray
            try {
                downloadTime = measureTimeMillis {
                    audio = VCSpeaker.voicetext.generateSpeech(text, voice)
                }
            } catch (exception: Exception) {
                message?.reply {
                    embed {
                        title = ":interrobang: Error!"

                        description = """
                            音声の生成に失敗しました。
                            「${message.content}」はよくわからない文字列ではありませんか？
                        """.trimIndent()

                        field("Exception") {
                            "```\n${exception.message ?: "不明"}\n```"
                        }

                        errorColor()
                    }
                }

                val messageInfoDetail = when (type) {
                    TrackType.System -> "the system message \"$text\""
                    TrackType.User -> "the message \"$text\" by @${message?.author?.username ?: "unknown_member"}"
                }

                logger.error(exception) {
                    "[$guildName] Failed to Generate Speech: Audio generation for $messageInfoDetail failed."
                }

                return
            }

            logger.info {
                "[$guildName] Audio Downloaded: Audio for $messageInfo has been downloaded in $downloadTime ms."
            }

            CacheStore.create(text, voice, audio)
        } else {
            logger.info {
                "[$guildName] Audio Found: Audio for $messageInfo has been found in the cache."
            }

            CacheStore.read(text, voice)!!
        }

        val info = SpeakInfo(message, guild, text, voice, file, type)

        if (queue.isEmpty() && now == null) {
            now = info
            speak(info)

            logger.info {
                "[$guildName] First Track Starting: Queue is empty. Audio track for $messageInfo skipped queue."
            }
        } else {
            queue.add(info)

            logger.info {
                "[$guildName] Track Queued: Audio track for $messageInfo has been queued. Waiting for ${queue.size} track(s) to finish playing."
            }
        }
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
            val info = track.userData as SpeakInfo
            val message = info.message
            val guildName = info.guild.name

            launch {
                message?.deleteOwnReaction(ReactionEmoji.Unicode("🔊"))
            }

            if (endReason.mayStartNext && queue.isNotEmpty()) {
                now = queue.removeFirst()
                launch { speak(now!!) }

                logger.info {
                    "[$guildName] Next Track Starting: Audio track for ${info.getMessageLogInfo()} popped out from the queue."
                }
            } else {
                now = null

                logger.info {
                    "[$guildName] Playing Track Finished: All tracks have been played. Waiting for the next track..."
                }
            }
        }

    private suspend fun speak(info: SpeakInfo): Unit = runBlocking {
        launch {
            if (info.message != null) info.message.addReaction("🔊")
        }
        player.speak(info)
    }
}