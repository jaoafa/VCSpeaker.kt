package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kordex.core.commands.application.slash.PublicSlashCommand
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.publicSubCommand
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.publicSlashCommand

object SlashCommandExtensions {
    private fun PublicSlashCommand<*, *>.devGuild() {
        val devGuildId = VCSpeaker.devGuildId
        if (devGuildId != null) guild(devGuildId)
    }

    // Command Extension Functions
    @JvmName("namedPublicSlashCommandWithOptionModal")
    suspend fun <O : Options, M : ModalForm> Extension.publicSlashCommand(
        name: String,
        description: String,
        options: () -> O,
        modal: () -> M,
        builder: suspend PublicSlashCommand<O, M>.() -> Unit
    ) {
        publicSlashCommand(options, modal) {
            this.name = if (VCSpeaker.isDev()) "dev-$name" else name
            this.description = description

            devGuild()

            apply { builder() }
        }
    }

    @JvmName("namedPublicSlashCommandWithModal")
    suspend fun <M : ModalForm> Extension.publicSlashCommand(
        name: String,
        description: String,
        modal: () -> M,
        builder: suspend PublicSlashCommand<Options, M>.() -> Unit
    ) {
        publicSlashCommand(modal) {
            this.name = if (VCSpeaker.isDev()) "dev-$name" else name
            this.description = description

            devGuild()

            apply { builder() }
        }
    }

    @JvmName("namedPublicSlashCommandWithOption")
    suspend fun <O : Options> Extension.publicSlashCommand(
        name: String,
        description: String,
        options: () -> O,
        builder: suspend PublicSlashCommand<O, ModalForm>.() -> Unit
    ) {
        publicSlashCommand(options) {
            this.name = if (VCSpeaker.isDev()) "dev-$name" else name
            this.description = description

            devGuild()

            apply { builder() }
        }
    }

    @JvmName("namedPublicSlashCommand")
    suspend fun Extension.publicSlashCommand(
        name: String,
        description: String,
        builder: suspend PublicSlashCommand<Options, ModalForm>.() -> Unit
    ) {
        publicSlashCommand {
            this.name = if (VCSpeaker.isDev()) "dev-$name" else name
            this.description = description

            devGuild()

            apply { builder() }
        }
    }

    // Subcommand Extension Functions
    @JvmName("namedPublicSubCommandWithOptionModal")
    suspend fun <O : Options, M : ModalForm> SlashCommand<*, *, *>.publicSubCommand(
        name: String,
        description: String,
        options: () -> O,
        modal: () -> M,
        builder: suspend PublicSlashCommand<O, M>.() -> Unit
    ) {
        publicSubCommand(options, modal) {
            this.name = name
            this.description = description

            apply { builder() }
        }
    }

    @JvmName("namedPublicSubCommandWithModal")
    suspend fun <M : ModalForm> SlashCommand<*, *, *>.publicSubCommand(
        name: String,
        description: String,
        modal: () -> M,
        builder: suspend PublicSlashCommand<Options, M>.() -> Unit
    ) {
        publicSubCommand(modal) {
            this.name = name
            this.description = description

            apply { builder() }
        }
    }

    @JvmName("namedPublicSubCommandWithOption")
    suspend fun <O : Options> SlashCommand<*, *, *>.publicSubCommand(
        name: String,
        description: String,
        options: () -> O,
        builder: suspend PublicSlashCommand<O, ModalForm>.() -> Unit
    ) {
        publicSubCommand(options) {
            this.name = name
            this.description = description

            apply { builder() }
        }
    }

    @JvmName("namedPublicSubCommand")
    suspend fun SlashCommand<*, *, *>.publicSubCommand(
        name: String,
        description: String,
        builder: suspend PublicSlashCommand<Options, ModalForm>.() -> Unit
    ) {
        publicSubCommand {
            this.name = name
            this.description = description

            apply { builder() }
        }
    }
}