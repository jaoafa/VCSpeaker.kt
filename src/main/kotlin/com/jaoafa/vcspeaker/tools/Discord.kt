package com.jaoafa.vcspeaker.tools

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.chat.ChatCommandContext
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.PublicInteractionContext
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.respond
import com.kotlindiscord.kord.extensions.utils.selfMember
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed

typealias Options = Arguments

object Discord {
    fun Guild.autoJoinEnabled() = GuildStore.getOrDefault(this.id).autoJoin

    suspend fun BaseVoiceChannelBehavior.isAfk() = this.getGuild().afkChannel == this

    private fun PublicSlashCommand<*, *>.devGuild() {
        val devGuildId = VCSpeaker.devId
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

    suspend fun PublicInteractionContext.respond(
        content: String
    ) = this.respond {
        this.content = content
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

    suspend inline fun <reified T : Channel> Snowflake.asChannelOf() = VCSpeaker.kord.getChannelOf<T>(this)

    suspend infix fun VoiceChannel?.orMembersCurrent(member: MemberBehavior) =
        this ?: member.getVoiceStateOrNull()?.getChannelOrNull()

    suspend fun GuildBehavior.selfVoiceChannel() = selfMember().getVoiceStateOrNull()?.getChannelOrNull()

    suspend fun ChatCommandContext<out Arguments>.respond(content: String) = message.respond(content)

    suspend fun BaseVoiceChannelBehavior.name() = this.asChannel().name
}