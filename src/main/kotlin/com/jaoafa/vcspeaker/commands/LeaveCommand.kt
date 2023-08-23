package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordVoice

class LeaveCommand : Extension() {

    override val name = this::class.simpleName!!

    @OptIn(KordVoice::class)
    override suspend fun setup() {
        publicSlashCommand("leave", "VC から退出します。") {
            action {
                val guildId = guild!!.id
                val narrator = VCSpeaker.narrators[guildId] ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                narrator.connection.leave()
                narrator.player.destroy()

                VCSpeaker.narrators.remove(guildId)

                respond("**:wave: 切断しました。**")
            }
        }
    }
}