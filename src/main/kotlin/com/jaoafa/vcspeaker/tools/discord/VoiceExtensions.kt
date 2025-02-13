package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.name
import com.jaoafa.vcspeaker.tts.Speech
import com.jaoafa.vcspeaker.tts.narrators.NarrationScripts
import com.jaoafa.vcspeaker.tts.narrators.Narrator
import com.jaoafa.vcspeaker.tts.narrators.Narrators
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import dev.kord.common.annotation.KordVoice
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.voice.AudioFrame
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.TimeUnit

object VoiceExtensions {
    private val logger = KotlinLogging.logger { }

    /**
     * VoiceChannel に接続します。
     *
     * 接続時に [Narrator] が作成され、[Narrators] に登録されます。
     *
     * @param replier 参加通知の文章を受け取り、返信する関数
     */
    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.join(
        replier: (suspend (String) -> Unit)? = null
    ): Narrator {
        Narrators -= guild.id // force disconnection

        val player = VCSpeaker.lavaplayer.createPlayer()

        val connection = connect {
            audioProvider {
                AudioFrame.fromData(player.provide(1, TimeUnit.SECONDS)?.data)
            }
        }

        val narrator = Narrator(guild.id, player, connection)
        Narrators += narrator

        narrator.announce(
            NarrationScripts.SELF_JOIN,
            "**:loudspeaker: $mention に接続しました。**",
            replier
        )

        val name = name()
        val guildName = guild.asGuild().name

        logger.info {
            "[$guildName] Joined: Joined to $name"
        }

        return narrator
    }

    /**
     * 新しい VoiceChannel へ移動します。
     *
     * 実行時に VoiceChannel に接続していない場合、何も起こりません。
     *
     * @param replier 移動通知の文章を受け取り、返信する関数
     */
    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.move(
        replier: (suspend (String) -> Unit)? = null
    ): Narrator? {
        val narrator = guild.narrator() ?: return null

        narrator.connection.move(id)

        narrator.announce(
            NarrationScripts.SELF_MOVE,
            "**:loudspeaker: $mention に移動しました。**",
            replier
        )

        val name = name()
        val guildName = guild.asGuild().name

        logger.info {
            "[$guildName] Moved: Moved to $name"
        }

        return narrator
    }

    /**
     * VoiceChannel から退出します。
     *
     * 実行時に VoiceChannel に接続していない場合、何も起こりません。
     *
     * 退出時に [Narrator] が破棄され、[Narrators] から削除されます。
     *
     * @param replier 退出通知の文章を受け取り、返信する関数
     */
    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.leave(
        replier: (suspend (String) -> Unit)? = null
    ) {
        val narrator = guild.narrator() ?: return

        narrator.connection.leave()
        narrator.player.destroy()

        Narrators -= guild.id

        narrator.announce(
            "",
            "**:wave: $mention から退出しました。**",
            replier
        )

        val name = name()
        val guildName = guild.asGuild().name

        logger.info {
            "[$guildName] Left: Left from $name"
        }
    }

    fun AudioPlayer.speak(speech: Speech) {
        val guildName = speech.guild.name

        try {
            this.playTrack(speech.tracks[0])

            logger.info {
                "[$guildName] Playing Track: Audio for ${speech.describe()} is playing now (${speech.tracks[0].identifier})"
            }
        } catch (exception: Exception) {
            logger.error(exception) {
                "[$guildName] Failed to Play Track: Audio track for ${speech.describe(withText = true)} have failed to play."
            }
        }
    }
}