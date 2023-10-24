package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.ChatCommandExtensions.chatCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orFallbackOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.move
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ChannelType

class JoinCommand : Extension() {
    override val name = this::class.simpleName!!

    inner class JoinOptions : Options() {
        val channel by optionalChannel {
            name = "channel"
            description = "参加する VC"
            requireChannelType(ChannelType.GuildVoice)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("join", "VC に接続します。", ::JoinOptions) {
            action {
                // option > member's voice channel > error
                val channel = arguments.channel.orFallbackOf(member!!) {
                    respond(it)
                } ?: return@action

                val selfChannel = guild!!.selfVoiceChannel()
                val replier: suspend (String) -> Unit = { respond(it) }

                if (selfChannel != null) channel.move(replier)
                else channel.join(replier)
            }
        }

        chatCommand("join", "VC に接続します。") {
            aliases += "summon"

            action {
                val channel = member!!.getVoiceStateOrNull()?.getChannelOrNull() ?: run {
                    respond("**:question: VC に参加してください。**")
                    return@action
                }

                val selfChannel = guild!!.selfVoiceChannel()
                val replier: suspend (String) -> Unit = { respond(it) }

                if (selfChannel != null) channel.move(replier)
                else channel.join(replier)
            }
        }
    }
}