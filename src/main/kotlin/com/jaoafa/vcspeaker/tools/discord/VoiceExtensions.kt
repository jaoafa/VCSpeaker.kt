package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.voicetext.NarrationScripts
import com.jaoafa.vcspeaker.voicetext.Narrator
import com.jaoafa.vcspeaker.voicetext.Narrators
import com.jaoafa.vcspeaker.voicetext.Narrators.narrator
import dev.kord.common.annotation.KordVoice
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.voice.AudioFrame
import java.util.concurrent.TimeUnit

object VoiceExtensions {
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
    }
}