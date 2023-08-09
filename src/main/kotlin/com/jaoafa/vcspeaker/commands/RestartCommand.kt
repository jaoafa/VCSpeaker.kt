package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.devGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import kotlin.system.exitProcess

class RestartCommand : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        publicSlashCommand {
            name = "restart"
            description = "VCSpeaker を再起動します。"

            devGuild()

            action {
                respond { content = ":firecracker: **再起動します。**" }
                event.kord.shutdown()
                exitProcess(0)
            }
        }
    }
}