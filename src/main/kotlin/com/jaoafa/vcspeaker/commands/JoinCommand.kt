package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker.join
import com.jaoafa.vcspeaker.VCSpeaker.move
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.jaoafa.vcspeaker.tools.Options
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.selfMember
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
                val targetChannel = arguments.target?.asChannelOf<VoiceChannel>()
                    ?: member!!.getVoiceStateOrNull()?.getChannelOrNull()
                    ?: run {
                        respond("**:question: VC に参加、または指定してください。**")
                        return@action
                    }

                val selfChannel = guild!!.selfMember().getVoiceStateOrNull()?.getChannelOrNull()

                if (selfChannel != null) targetChannel.move(this)
                else targetChannel.join(this)
            }
        }
    }
}