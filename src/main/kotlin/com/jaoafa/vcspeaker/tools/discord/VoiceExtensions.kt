package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.voicetext.NarrationScripts
import com.jaoafa.vcspeaker.voicetext.Narrator
import com.jaoafa.vcspeaker.voicetext.NarratorExtensions.announce
import com.jaoafa.vcspeaker.voicetext.Narrators
import com.jaoafa.vcspeaker.voicetext.Narrators.narrator
import com.kotlindiscord.kord.extensions.types.PublicInteractionContext
import dev.kord.common.annotation.KordVoice
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.Message
import dev.kord.voice.AudioFrame
import java.util.concurrent.TimeUnit

object VoiceExtensions {

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.join(
        interaction: PublicInteractionContext? = null,
        message: Message? = null
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
            interaction,
            message
        )

        return narrator
    }

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.move(
        interaction: PublicInteractionContext? = null,
        message: Message? = null
    ): Narrator? {
        val narrator = guild.narrator() ?: return null

        narrator.connection.move(id)

        narrator.announce(
            NarrationScripts.SELF_MOVE,
            "**:loudspeaker: $mention に移動しました。**",
            interaction,
            message
        )

        return narrator
    }

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.leave(
        interaction: PublicInteractionContext? = null,
        message: Message? = null
    ) {
        val narrator = guild.narrator() ?: return

        narrator.connection.leave()
        narrator.player.destroy()

        Narrators -= guild.id

        narrator.announce(
            "",
            "**:wave: $mention から退出しました。**",
            interaction,
            message
        )
    }
}