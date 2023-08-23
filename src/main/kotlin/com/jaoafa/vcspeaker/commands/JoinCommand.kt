package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.jaoafa.vcspeaker.tools.Options
import com.jaoafa.vcspeaker.voicetext.NarrationScripts
import com.jaoafa.vcspeaker.voicetext.Narrator
import com.jaoafa.vcspeaker.voicetext.NarratorExtensions.speakSelf
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.selfMember
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.channel.VoiceChannelBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.voice.AudioFrame

class JoinCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class JoinOptions : Options() {
        val target by optionalChannel {
            name = "channel"
            description = "参加する VC"
            requireChannelType(ChannelType.GuildVoice)
        }
    }

    @OptIn(KordVoice::class)
    override suspend fun setup() {
        publicSlashCommand("join", "VC に接続します。", ::JoinOptions) {
            action {
                // option > member's voice channel > error
                val target = arguments.target?.asChannelOf<VoiceChannel>()
                    ?: member!!.getVoiceStateOrNull()?.getChannelOrNull()
                    ?: run {
                        respond("**:question: VC に参加、または指定してください。**")
                        return@action
                    }

                // force disconnection
                if (guild!!.selfMember().getVoiceStateOrNull()?.getChannelOrNull() == null)
                    VCSpeaker.narrators.remove(guild!!.id)

                val narrator = VCSpeaker.narrators[guild!!.id]

                if (narrator != null) { // already connected
                    narrator.connection.move(target.id)
                    narrator.player.speakSelf(NarrationScripts.SELF_MOVE, guild!!.id)
                    respond("**:arrow_right: ${target.mention} に移動しました。**")
                    return@action
                }

                val player = VCSpeaker.lavaplayer.createPlayer()
                val guildId = guild!!.id

                player.speakSelf(NarrationScripts.SELF_JOIN, guildId) // DO NOT REMOVE

                val connection = (target as VoiceChannelBehavior).connect {
                    audioProvider {
                        AudioFrame.fromData(player.provide()?.data ?: ByteArray(0))
                    }
                }

                VCSpeaker.narrators[guildId] = Narrator(guildId, player, connection)

                respond("**:loud_sound: ${target.mention} に接続しました。**")
            }
        }
    }
}