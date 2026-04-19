package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.addReactionSafe
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.deleteOwnReactionSafe
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.speak
import com.jaoafa.vcspeaker.tts.providers.BatchProvider
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackEndEvent.AudioTrackEndReason
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.embed
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.audio.TrackEndEvent
import dev.schlaubi.lavakord.audio.on
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.*
import kotlinx.io.IOException


class Scheduler(
    private val link: Link,
    /**
     * 再生中・再生待ちの Speech の Queue.
     * 0 番目が現在再生中の Speech です。
     */
    val queue: MutableList<Speech> = mutableListOf()
) {
    private val logger = KotlinLogging.logger { }

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
            BatchProvider(link, contexts).start()
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

        val speech = Speech(actor, guild.name, message, contexts, tracks)

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

    suspend fun start() {
        val next = queue.removeFirst()
        beginSpeech(next)

        logger.info {
            "[${next.guildName}] Speech Starting: The speech for ${next.describe()} has been started."
        }
    }

    suspend fun skip() {
        if (queue.isEmpty()) {
            link.player.stopTrack()
        } else {
            val next = queue.removeFirst()
            beginSpeech(next)
        }
    }

    suspend fun onTrackEnd(
        endReason: AudioTrackEndReason
    ) {
        val message = current()!!.message
        val guildName = current()!!.guildName

        val next = current()!!.next()

        // Speech 内に次の Track が存在し、かつ再生が可能な場合、次の Track を再生
        if (endReason.mayStartNext && next != null) {
            val (nextTrack, _) = next

            link.player.playTrack(nextTrack)
            return
        }

        message?.deleteOwnReactionSafe("🔊")

        queue.removeFirst()
        val nextSpeech = current()

        // Speech 内のすべての Track を再生し終わった場合、次の Speech を再生
        if (endReason.mayStartNext && nextSpeech != null) {
            beginSpeech(nextSpeech)

            logger.info {
                "[$guildName] Next Speech Starting: The speech for ${nextSpeech.describe()} has been started."
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
    suspend fun beginSpeech(speech: Speech) {
        speech.message?.addReactionSafe("🔊")
        link.player.speak(speech)
    }

    init {
        link.player.on<TrackEndEvent> event@{
            onTrackEnd(reason)
        }
    }
}