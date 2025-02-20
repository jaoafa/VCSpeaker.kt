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
import io.ktor.client.plugins.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException

class Scheduler(
    private val player: AudioPlayer
) : AudioEventAdapter() {
    private val logger = KotlinLogging.logger { }

    /**
     * 再生中・再生待ちの Speech の Queue.
     * 0 番目が現在再生中の Speech です。
     */
    val queue = mutableListOf<Speech>()

    /**
     * 現在再生中の Speech を取得します。
     */
    fun current(): Speech? = queue.firstOrNull()

    /**
     * Queue に Speech を追加します。
     *
     * @param contexts [ProviderContext] のリスト
     * @param message メッセージ
     * @param guild サーバー
     * @param actor 読み上げの種類
     */
    suspend fun <T : ProviderContext> queue(
        contexts: List<T>, message: Message? = null, guild: Guild, actor: SpeechActor
    ) {
        val guildName = guild.name

        val messageInfo = "the message by @${message?.author?.username ?: "unknown_member"}"

        val tracks = try {
            BatchProvider(contexts).start()
        } catch (exception: HttpRequestTimeoutException) {
            message?.reply {
                embed {
                    title = ":interrobang: Error!"

                    description = """
                        メッセージを読み上げられません。
                        リクエストがタイムアウトしました。
                    """.trimIndent()

                    errorColor()
                }
            }

            logger.error(exception) {
                "[$guildName] Request Timed Out."
            }

            return
        } catch (exception: IOException) {
            message?.reply {
                embed {
                    title = ":interrobang: Error!"

                    description = """
                        メッセージを読み上げられません。
                        リクエストが失敗しました。
                    """.trimIndent()

                    field("Exception") {
                        "```\n${exception::class.simpleName}:\n${exception.message ?: "不明"}\n```"
                    }

                    errorColor()
                }
            }

            logger.error(exception) {
                "[$guildName] Request Failed."
            }

            return
        } catch (exception: Exception) {
            message?.reply {
                embed {
                    title = ":interrobang: Error!"

                    description = """
                        メッセージを読み上げられません。
                        「${message.content}」がよくわからない文字列であるか、音声の生成に失敗した可能性があります。
                    """.trimIndent()

                    field("Exception") {
                        "```\n${exception::class.simpleName}:\n${exception.message ?: "不明"}\n```"
                    }

                    errorColor()
                }
            }

            val messageInfoDetail = when (actor) {
                SpeechActor.System -> "the system message \"${message?.content}\""
                SpeechActor.User -> "the message \"${message?.content}\" by @${message?.author?.username ?: "unknown_member"}"
            }

            logger.error(exception) {
                "[$guildName] Failed to Generate Speech: Generating the speech for $messageInfoDetail failed."
            }

            return
        }

        val speech = Speech(actor, guild, message, contexts, tracks)

        queue.add(speech)

        if (queue.size == 1) {
            beginSpeech(speech)

            logger.info {
                "[$guildName] Speech Starting: The queue is empty. The speech for $messageInfo skipped the queue."
            }
        } else {
            logger.info {
                "[$guildName] Speech Queued: The speech for $messageInfo has been queued. Waiting for ${queue.size} speech(es) to finish playing."
            }
        }
    }

    fun skip() {
        if (queue.isEmpty()) {
            player.stopTrack()
        } else {
            val next = queue.removeFirst()
            beginSpeech(next)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason): Unit =
        runBlocking {
            val message = current()!!.message
            val guildName = current()!!.guild.name

            val nextTrack = current()!!.next()

            // Speech 内に次の Track が存在し、かつ再生が可能な場合、次の Track を再生
            if (endReason.mayStartNext && nextTrack != null) {
                launch { player.playTrack(nextTrack) }
                return@runBlocking
            }

            launch {
                message?.deleteOwnReaction(ReactionEmoji.Unicode("🔊"))
            }

            queue.removeFirst()
            val next = current()

            // Speech 無いのすべての Track を再生し終わった場合、次の Speech を再生
            if (endReason.mayStartNext && next != null) {
                launch { beginSpeech(next) }

                logger.info {
                    "[$guildName] Next Speech Starting: The speech for ${next.describe()} has been started."
                }
            } else {
                logger.info {
                    "[$guildName] Speech Finished: All tracks have been played. Waiting for the next speech..."
                }
            }
        }

    /**
     * 音声を再生します。
     *
     * @param speech 音声
     */
    private fun beginSpeech(speech: Speech): Unit = runBlocking {
        launch {
            if (speech.message != null) speech.message.addReaction("🔊")
        }
        player.speak(speech)
    }
}