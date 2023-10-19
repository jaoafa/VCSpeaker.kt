package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.leave
import com.jaoafa.vcspeaker.tools.discord.CommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand

class LeaveCommand : Extension() {

    override val name = this::class.simpleName!!

    override suspend fun setup() {
        publicSlashCommand("leave", "VC から退出します。") {
            action {
                val selfChannel = guild!!.selfVoiceChannel() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                selfChannel.leave(interaction = this)
            }
        }

        chatCommand {
            name = "leave"
            description = "VC から退出します。"
            aliases += "disconnect"

            action {
                val selfChannel = guild!!.selfVoiceChannel() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                selfChannel.leave(message = message)
            }
        }
    }
}