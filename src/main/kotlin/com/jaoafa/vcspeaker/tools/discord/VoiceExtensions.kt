package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.name
import com.jaoafa.vcspeaker.tts.Speech
import com.jaoafa.vcspeaker.tts.narrators.NarrationScripts
import com.jaoafa.vcspeaker.tts.narrators.Narrator
import com.jaoafa.vcspeaker.tts.narrators.NarratorManager
import com.jaoafa.vcspeaker.tts.narrators.NarratorManager.getNarrator
import dev.kord.common.annotation.KordVoice
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.schlaubi.lavakord.audio.player.Player
import dev.schlaubi.lavakord.kord.connectAudio
import io.github.oshai.kotlinlogging.KotlinLogging

object VoiceExtensions {
    private val logger = KotlinLogging.logger { }

    /**
     * VoiceChannel に接続します。
     *
     * 接続時に [Narrator] が作成され、[NarratorManager] に登録されます。
     *
     * @param replier 参加通知の文章を受け取り、返信する関数
     */
    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.join(
        replier: (suspend (String) -> Unit)? = null
    ): Narrator {
        val connector = NarratorManager.prepareAdd(guild.id, this.id)

        val narrator = connector()

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
        val narrator = guild.getNarrator() ?: return null

        narrator.link.connectAudio(id)

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
     * 退出時に [Narrator] が破棄され、[NarratorManager] から削除されます。
     *
     * @param replier 退出通知の文章を受け取り、返信する関数
     */
    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.leave(
        replier: (suspend (String) -> Unit)? = null
    ) {
        val narrator = guild.getNarrator() ?: return

        val disconnecter = NarratorManager.remove(guild.id)

        disconnecter?.invoke()

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

    suspend fun Player.speak(speech: Speech) {
        val guildName = speech.guildName

        try {
            // applyFilters { // FIXME: StackOverflowError
            //     volume = if (speech.contexts[0] is SoundmojiContext) 20F else 100F
            // }

            this.playTrack(speech.tracks[0])

            logger.info {
                "[$guildName] Playing Track: Audio for ${speech.describe()} is playing now "// (${speech.tracks[0].identifier})"
            }
        } catch (exception: Exception) {
            logger.error(exception) {
                "[$guildName] Failed to Play Track: Audio track for ${speech.describe(withText = true)} have failed to play."
            }
        }
    }
}