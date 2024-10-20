package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.ChatCommandExtensions.chatCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.leave
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.respond

class LeaveCommand : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        publicSlashCommand("leave", "VC から退出します。") {
            check { anyGuild() }
            action {
                val selfChannel = guild!!.selfVoiceChannel() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                selfChannel.leave { respond(it) }
            }
        }

        chatCommand("leave", "VC から退出します。") {
            aliases += "disconnect"

            check {
                anyGuild()
                isNotBot()
            }
            action {
                val selfChannel = guild!!.selfVoiceChannel() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                selfChannel.leave { message.respond(it) }
            }
        }
    }
}