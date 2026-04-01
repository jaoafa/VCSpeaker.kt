package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.database.actions.GuildAction.fetchEntityOrNull
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.Event
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.failed
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.types.CheckContext
import io.github.oshai.kotlinlogging.KotlinLogging

suspend fun <T : Event> CheckContext<T>.anyGuildRegistered() {
    if (!passed) return

    val logger = KotlinLogging.logger {}

    val guild = guildFor(event) ?: run {
        logger.failed("Not in guild.")

        if (event is ChatInputCommandInteractionCreateEvent) {
            // Apparently smart cast is impossible as Event is declared in a different module
            (event as ChatInputCommandInteractionCreateEvent).interaction.respondEphemeral {
                embed(EmbedTemplates.NotInGuild().build())
            }
        }

        fail()
        return
    }

    if (guild.fetchEntityOrNull() == null) {
        logger.failed("Guild not registered.")

        if (event is ChatInputCommandInteractionCreateEvent) {
            (event as ChatInputCommandInteractionCreateEvent).interaction.respondEphemeral {
                embed(EmbedTemplates.GuildNotRegistered().build())
            }
        }

        fail()
        return
    }

    pass()
}
