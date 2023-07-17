package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.devGuild
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
            description = "VC から退出します。"

            devGuild()

            check {
                isNotBot()
            }

            action {
                val guildId = guild!!.id
                val narrator = VCSpeaker.narrators[guildId] ?: run {
                    respond { content = "**:question: VC に参加していません。**" }
                    return@action
                }

                narrator.connection.leave()
                narrator.player.destroy()

                VCSpeaker.narrators.remove(guildId)

                respond { content = "**:wave: 切断しました。**" }
            }
        }
    }
}