package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.asChannelOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tts.Narrators.narrator
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.runBlocking
import java.rmi.UnexpectedException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object NarratorExtensions {
    suspend fun Guild.announce(
        voice: String,
        text: String,
        replier: (suspend (String) -> Unit)? = null
    ) {
        if (replier != null) {
            replier(text)
        } else {
            val channel = GuildStore.getOrDefault(id).channelId?.asChannelOf<TextChannel>()
            channel?.createMessage(text)
        }

        narrator()?.queueSelf(voice)
    }

    suspend fun AudioPlayer.speak(info: SpeakInfo) {
        val track = suspendCoroutine {
            VCSpeaker.lavaplayer.loadItemOrdered(
                this,
                info.file.path, // already checked
                object : AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        track.userData = info
                        it.resume(track)
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist?) {
                        throw UnexpectedException("This code should not be reached.")
                    }

                    override fun noMatches() {
                        return
                    }

                    override fun loadFailed(exception: FriendlyException?): Unit = runBlocking {
                        info.message?.reply {
                            embed {
                                title = ":interrobang: Error!"

                                description = """
                                        音声の読み込みに失敗しました。
                                        VCSpeaker の不具合と思われる場合は、[GitHub Issues](https://github.com/jaoafa/VCSpeaker.kt/issues) か、サーバー既定のチャンネルへの報告をお願いします。
                                    """.trimIndent()

                                field("Exception") {
                                    "```\n${exception?.message ?: "不明"}\n```"
                                }

                                errorColor()
                            }
                        }
                    }

                })
        }

        this.playTrack(track)
    }
}