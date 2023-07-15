package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordVoice

class LeaveCommand : Extension() {
    override val name = "LeaveCommand"

    @OptIn(KordVoice::class)
    override suspend fun setup() {
        publicSlashCommand {
            name = "leave"
            description = "Leaves the voice channel."

            guild(839462224505339954)

            check {
                isNotBot()
            }

            action {
                val connection = VCSpeaker.connections[guild!!.id] ?: run {
                    respond { content = "**:question: VC に参加していません。**" }
                    return@action
                }

                val player = VCSpeaker.guildPlayer[guild!!.id]

                connection.leave()
                player?.destroy()

                VCSpeaker.run {
                    connections.remove(guild!!.id)
                    guildPlayer.remove(guild!!.id)
                }

                respond { content = "**:wave: 切断しました。**" }
            }
        }
    }
}