package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker.leave
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.selfMember
import dev.kord.common.annotation.KordVoice

class LeaveCommand : Extension() {

    override val name = this::class.simpleName!!

    override suspend fun setup() {
        publicSlashCommand("leave", "VC から退出します。") {
            action {
                val selfChannel = guild!!.selfMember().getVoiceStateOrNull()?.getChannelOrNull() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                selfChannel.leave(this)
            }
        }
    }
}