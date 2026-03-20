package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.database.DatabaseUtil.getEntityOrNull
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.failed
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.types.CheckContext
import io.github.oshai.kotlinlogging.KotlinLogging

suspend fun CheckContext<ChatInputCommandInteractionCreateEvent>.isGuildRegistered() {
    if (!passed) return

    val logger = KotlinLogging.logger("com.jaoafa.vcspeaker.tools.discord.isGuildRegistered")

    val guild = guildFor(event)

    if (guild == null) {
        logger.failed("Not in guild.")

        event.interaction.respondEphemeral {
            embed(EmbedTemplates.NotInGuild().build())
        }

        fail()
        return
    }

    val guildEntity = guild.getEntityOrNull()

    if (guildEntity == null) {
        logger.failed("Guild not registered.")

        event.interaction.respondEphemeral {
            embed(EmbedTemplates.GuildNotRegistered().build())
        }

        fail()
        return
    }

    pass()
}