package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.devGuild
import com.jaoafa.vcspeaker.voicetext.GuildNarrator
import com.jaoafa.vcspeaker.voicetext.Narrator.speakSelf
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.channel.VoiceChannelBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.voice.AudioFrame

class JoinCommand : Extension() {
    override val name = "JoinCommand"

    inner class JoinOptions : Arguments() {
        val target by optionalChannel {
            name = "channel"
            description = "The voice channel to join."
            requireChannelType(ChannelType.GuildVoice)
        }
    }

    @OptIn(KordVoice::class)
    override suspend fun setup() {
        publicSlashCommand(::JoinOptions) {
            name = "join"
            description = "VC に接続します。"

            devGuild()

            check { isNotBot() }

            action {
                // option > member's voice channel > error
                val target =
                    arguments.target?.asChannelOf<VoiceChannel>() ?: member!!.getVoiceStateOrNull()?.getChannelOrNull()
                    ?: run {
                        respond { content = "**:question: VC に参加、または指定してください。**" }
                        return@action
                    }

                val player = VCSpeaker.lavaplayer.createPlayer()
                val guildId = guild!!.id

                player.speakSelf("接続しました。", guildId) // DO NOT REMOVE

                val connection = (target as VoiceChannelBehavior).connect {
                    audioProvider {
                        AudioFrame.fromData(player.provide()?.data ?: ByteArray(0))
                    }
                }

                VCSpeaker.narrators[guildId] = GuildNarrator(guildId, player, connection)

                respond { content = "**:loud_sound: ${target.mention} に接続しました。**" }
            }
        }
    }
}