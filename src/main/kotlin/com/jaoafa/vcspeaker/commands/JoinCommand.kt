package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker.join
import com.jaoafa.vcspeaker.VCSpeaker.move
import com.jaoafa.vcspeaker.tools.Discord.orMembersCurrent
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.jaoafa.vcspeaker.tools.Discord.selfVoiceChannel
import com.jaoafa.vcspeaker.tools.Options
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.channel.VoiceChannel

class JoinCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class JoinOptions : Options() {
        val target by optionalChannel {
            name = "channel"
            description = "参加する VC"
            requireChannelType(ChannelType.GuildVoice)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("join", "VC に接続します。", ::JoinOptions) {
            action {
                // option > member's voice channel > error
                val targetChannel = (arguments.target?.asChannelOf<VoiceChannel>() orMembersCurrent member!!)
                    ?: run {
                        respond("**:question: VC に参加、または指定してください。**")
                        return@action
                    }

                val selfChannel = guild!!.selfVoiceChannel()

                if (selfChannel != null) targetChannel.move(interaction = this)
                else targetChannel.join(interaction = this)
            }
        }

        chatCommand {
            name = "join"
            description = "VC に接続します。"
            aliases += "summon"

            action {
                val targetChannel = (member!!.getVoiceStateOrNull()?.getChannelOrNull() as VoiceChannel?) ?: run {
                    respond("**:question: VC に参加してください。**")
                    return@action
                }
                val selfChannel = guild!!.selfVoiceChannel()

                if (selfChannel != null) targetChannel.move(message = message)
                else targetChannel.join(message = message)
            }
        }
    }
}