package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.CommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orMembersCurrent
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.move
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand
import com.kotlindiscord.kord.extensions.utils.respond
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
                val replier: suspend (String) -> Unit = { respond(it) }

                if (selfChannel != null) targetChannel.move(replier)
                else targetChannel.join(replier)
            }
        }

        chatCommand {
            name = "join"
            description = "VC に接続します。"
            aliases += "summon"

            action {
                val targetChannel = member!!.getVoiceStateOrNull()?.getChannelOrNull() ?: run {
                    respond("**:question: VC に参加してください。**")
                    return@action
                }

                val selfChannel = guild!!.selfVoiceChannel()
                val replier: suspend (String) -> Unit = { respond(it) }

                if (selfChannel != null) targetChannel.move(replier)
                else targetChannel.join(replier)
            }
        }
    }
}