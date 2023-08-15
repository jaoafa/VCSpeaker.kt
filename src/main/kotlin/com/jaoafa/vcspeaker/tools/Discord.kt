package com.jaoafa.vcspeaker.tools

import com.jaoafa.vcspeaker.VCSpeaker
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed

typealias Options = Arguments

fun PublicSlashCommand<*, *>.devGuild() {
    val devGuildId = VCSpeaker.dev
    if (devGuildId != null) guild(devGuildId)
}

fun EmbedBuilder.authorOf(user: User) {
    author {
        name = user.username
        icon = user.avatar?.url
    }
}

suspend fun EmbedBuilder.authorOf(user: UserBehavior) = authorOf(user.asUser())

object EmbedColors {
    val success = Color(0xa6e3a1)
    val error = Color(0xf38ba8)
    val warning = Color(0xfab387)
    val info = Color(0xf5e0dc)
}

fun EmbedBuilder.successColor() {
    color = EmbedColors.success
}

fun EmbedBuilder.errorColor() {
    color = EmbedColors.error
}

fun EmbedBuilder.warningColor() {
    color = EmbedColors.warning
}

fun EmbedBuilder.infoColor() {
    color = EmbedColors.info
}

suspend fun PublicSlashCommandContext<*, *>.respondEmbed(
    title: String,
    description: String? = null,
    builder: suspend EmbedBuilder.() -> Unit = {}
) = this.respond {
    embed(title, description, builder)
}

suspend fun FollowupMessageCreateBuilder.embed(
    title: String,
    description: String? = null,
    builder: suspend EmbedBuilder.() -> Unit = {}
) = this.embed {
    this.title = title
    this.description = description
    apply { builder() }
}

// Command Extension Functions
//todo
suspend fun <O : Options, M : ModalForm> Extension.publicSlashCommand(
    name: String,
    description: String,
    options: () -> O,
    modal: () -> M,
    builder: suspend PublicSlashCommand<O, M>.() -> Unit
) {
    publicSlashCommand(options, modal) {
        this.name = name
        this.description = description

        apply { builder() }
    }
}

suspend fun <M : ModalForm> Extension.publicSlashCommand(
    name: String,
    description: String,
    modal: () -> M,
    builder: suspend PublicSlashCommand<Options, M>.() -> Unit
) {
    publicSlashCommand(modal) {
        this.name = name
        this.description = description

        apply { builder() }
    }
}

suspend fun <O : Options> Extension.publicSlashCommand(
    name: String,
    description: String,
    options: () -> O,
    builder: suspend PublicSlashCommand<O, ModalForm>.() -> Unit
) {
    publicSlashCommand(options) {
        this.name = name
        this.description = description

        apply { builder() }
    }
}

suspend fun Extension.publicSlashCommand(
    name: String,
    description: String,
    builder: suspend PublicSlashCommand<Options, ModalForm>.() -> Unit
) {
    publicSlashCommand {
        this.name = name
        this.description = description

        apply { builder() }
    }
}

// Subcommand Extension Functions

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