package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.speak
import com.jaoafa.vcspeaker.tts.providers.BatchProvider
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import dev.kordex.core.utils.addReaction
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

class Scheduler(
    private val player: AudioPlayer
) : AudioEventAdapter() {
    private val logger = KotlinLogging.logger { }

    val queue = mutableListOf<Speech>()
    var now: Speech? = null // Todo remove

    suspend fun <T : ProviderContext> queue(
        contexts: List<T>, message: Message? = null, guild: Guild, type: TrackType
    ) {
        val guildName = guild.name

        val messageInfo = "the message by @${message?.author?.username ?: "unknown_member"}"

        val tracks = try {
            BatchProvider(contexts).start()
        } catch (exception: Exception) {
            message?.reply {
                embed {
                    title = ":interrobang: Error!"

                    description = """
                        メッセージを読み上げられません。
                        「${message.content}」がよくわからない文字列であるか、音声の生成に失敗した可能性があります。
                    """.trimIndent()

                    field("Exception") {
                        "```\n${exception.message ?: "不明"}\n```"
                    }

                    errorColor()
                }
            }

            val messageInfoDetail = when (type) {
                TrackType.System -> "the system message \"${message?.content}\""
                TrackType.User -> "the message \"${message?.content}\" by @${message?.author?.username ?: "unknown_member"}"
            }

            logger.error(exception) {
                "[$guildName] Failed to Generate Speech: Audio generation for $messageInfoDetail failed."
            }

            return
        }

        val info = Speech(type, guild, message, contexts, tracks)

        if (queue.isEmpty() && now == null) {
            now = info
            beginSpeech(info)

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

    fun skip() {
        if (queue.isEmpty()) {
            player.stopTrack()
        } else {
            val next = queue.removeFirst()
            now = next
            beginSpeech(next)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason): Unit =
        runBlocking {
            if (now == null) {
                logger.info {
                    "Playing Track Finished: No track is playing. Waiting for the next track..."
                }
                return@runBlocking
            }

            val message = now!!.message
            val guildName = now!!.guild.name

            val nextTrack = now!!.next()

            if (endReason.mayStartNext && nextTrack != null) {
                launch { player.playTrack(nextTrack) }
                return@runBlocking
            }

            launch {
                message?.deleteOwnReaction(ReactionEmoji.Unicode("🔊"))
            }

            if (endReason.mayStartNext && queue.isNotEmpty()) {
                now = queue.removeFirst()
                launch { beginSpeech(now!!) }

                logger.info {
                    "[$guildName] Next Track Starting: Audio track for ${now?.describe()} has been retrieved from the queue."
                }
            } else {
                now = null

                logger.info {
                    "[$guildName] Playing Track Finished: All tracks have been played. Waiting for the next track..."
                }
            }
        }

    private fun beginSpeech(speech: Speech): Unit = runBlocking {
        launch {
            if (speech.message != null) speech.message.addReaction("🔊")
        }
        player.speak(speech)
    }
}