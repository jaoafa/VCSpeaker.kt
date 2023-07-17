package com.jaoafa.vcspeaker.tools

import com.jaoafa.vcspeaker.VCSpeaker
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommand

fun PublicSlashCommand<*, *>.devGuild() {
    if (VCSpeaker.dev != null) guild(VCSpeaker.dev!!)
}