package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kordex.core.checks.inGuild
import dev.kordex.core.commands.chat.ChatCommand
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.chatCommand

object ChatCommandExtensions {
    private fun ChatCommand<*>.devGuild() {
        val devGuildId = VCSpeaker.devGuildId
        if (devGuildId != null) check { inGuild(devGuildId) }
    }

    @JvmName("namedChatCommandWithOptionModal")
    suspend fun <O : Options> Extension.chatCommand(
        name: String,
        description: String,
        options: () -> O,
        builder: suspend ChatCommand<O>.() -> Unit
    ) {
        chatCommand(options) {
            this.name = if (VCSpeaker.isDev()) "dev-$name" else name
            this.description = description

            devGuild()

            apply { builder() }
        }
    }

    @JvmName("namedChatCommandWithOption")
    suspend fun Extension.chatCommand(
        name: String,
        description: String,
        builder: suspend ChatCommand<Options>.() -> Unit
    ) {
        chatCommand {
            this.name = if (VCSpeaker.isDev()) "dev-$name" else name
            this.description = description

            devGuild()

            apply { builder() }
        }
    }
}