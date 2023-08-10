package com.jaoafa.vcspeaker.tools

import com.jaoafa.vcspeaker.VCSpeaker
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed

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
