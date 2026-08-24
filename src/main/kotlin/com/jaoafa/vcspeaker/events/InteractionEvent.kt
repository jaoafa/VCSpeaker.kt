package com.jaoafa.vcspeaker.events

import dev.kord.common.entity.InteractionType
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import io.github.oshai.kotlinlogging.KotlinLogging

class InteractionEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    override suspend fun setup() {
        event<InteractionCreateEvent> {
            action {
                val interaction = event.interaction
                val type = interaction.type

                if (type == InteractionType.Ping) return@action

                val name = interaction.data.data.name.value
                val user = interaction.user.username

                val typeName = when (type) {
                    InteractionType.ApplicationCommand -> "application command"
                    InteractionType.AutoComplete -> "auto complete"
                    InteractionType.Component -> "component"
                    InteractionType.ModalSubmit -> "modal"
                    else -> "unknown interaction"
                }

                val guild = interaction.data.guildId.value?.let { kord.getGuildOrNull(it) }
                val guildPrefix = if (guild != null) "[${guild.name}] " else ""

                logger.info {
                    guildPrefix + "Interaction Made: @$user made interaction to the $typeName: $name (interaction id: ${interaction.id})"
                }
            }
        }
    }
}