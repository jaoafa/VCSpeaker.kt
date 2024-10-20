package com.jaoafa.vcspeaker.tools.discord

import dev.kordex.core.commands.application.slash.PublicSlashCommandContext
import dev.kordex.core.commands.chat.ChatCommandContext
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import io.github.oshai.kotlinlogging.KLogger

object DiscordLoggingExtension {
    // use context receiver once it became stable

    suspend fun PublicSlashCommandContext<*, *>.log(logger: KLogger, message: (Guild, User) -> String) {
        val user = user.asUser()
        val guild = guild!!.asGuild()

        message(guild, user).let { logger.info { it } }
    }

    suspend fun PublicSlashCommandContext<*, *>.log(logger: KLogger, message: (User) -> String) {
        val user = user.asUser()

        message(user).let { logger.info { it } }
    }

    suspend fun ChatCommandContext<*>.log(logger: KLogger, message: (Guild, User) -> String) {
        val user = user!!.asUser()
        val guild = guild!!.asGuild()

        message(guild, user).let { logger.info { it } }
    }

    suspend fun ChatCommandContext<*>.log(logger: KLogger, message: (User) -> String) {
        val user = user!!.asUser()

        message(user).let { logger.info { it } }
    }
}