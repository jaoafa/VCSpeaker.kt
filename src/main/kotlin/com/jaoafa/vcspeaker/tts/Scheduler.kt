package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.states.State
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.speak
import dev.kordex.core.utils.addReaction
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.rest.builder.message.embed
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

class Scheduler(
    private val guildId: Snowflake,
    private val player: AudioPlayer
) : AudioEventAdapter() {
    private val logger = KotlinLogging.logger { }

    private fun getQueue() = State.queue.get()[guildId] ?: emptyList()

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

        val entry = Entry(message, guild, text, voice, file, type)

        if (getQueue().isEmpty()) {
            speak(entry)

            logger.info {
                "[$guildName] First Track Starting: Queue is empty. Audio track for $messageInfo skipped queue."
            }
        } else {
            State.queue.add(entry)

            logger.info {
                "[$guildName] Track Queued: Audio track for $messageInfo has been queued. Waiting for ${getQueue().size} track(s) to finish playing."
            }
        }
    }

    fun skip() {
        if (getQueue().isEmpty()) {
            player.stopTrack()
        } else {
            val next = State.queue.removeFirstOf(guildId)
            next?.let { speak(it) }
        }
    }

    fun clear() {
        CoroutineScope(Dispatchers.Default).launch {
            getQueue().forEach {
                it.message?.deleteOwnReaction(ReactionEmoji.Unicode("🔊"))
                it.message?.deleteOwnReaction(ReactionEmoji.Unicode("👀"))
            }
        }

        State.queue.modify {
            it.toMutableMap().apply {
                this[guildId] = emptyList()
            }
        }
        player.stopTrack()
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason): Unit =
        runBlocking {
            val info = track.userData as Entry
            val message = info.message
            val guildName = info.guild.name

            launch {
                message?.deleteOwnReaction(ReactionEmoji.Unicode("🔊"))
            }

            if (endReason.mayStartNext && getQueue().isNotEmpty()) {
                val next = State.queue.removeFirstOf(guildId)
                launch { speak(next!!) }

                logger.info {
                    "[$guildName] Next Track Starting: Audio track for ${info.getMessageLogInfo()} has been retrieved from the queue."
                }
            } else {
                logger.info {
                    "[$guildName] Playing Track Finished: All tracks have been played. Waiting for the next track..."
                }
            }
        }

    private fun speak(entry: Entry): Unit = runBlocking {
        launch {
            if (entry.message != null) entry.message.addReaction("🔊")
        }
        player.speak(entry)
    }
}