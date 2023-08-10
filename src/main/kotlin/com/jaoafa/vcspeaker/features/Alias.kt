package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.store.AliasStore
import com.jaoafa.vcspeaker.store.AliasType
import com.kotlindiscord.kord.extensions.utils.FilterStrategy
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.core.entity.interaction.AutoCompleteInteraction
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder

class Alias {
    companion object {
        val autocomplete: suspend AutoCompleteInteraction.(AutoCompleteInteractionCreateEvent) -> Unit = { event ->
            val guildId = event.interaction.getChannel().data.guildId.value

            suggestStringMap(
                AliasStore.data.filter { it.guildId == guildId }
                    .associate { "${it.type.displayName} / ${it.from} → ${it.to}" to it.from },
                FilterStrategy.Contains
            )
        }

        fun EmbedBuilder.fieldAliasFrom(type: AliasType, from: String) = this.field(":mag: ${type.displayName}", true) {
            when (type) {
                AliasType.Text -> from
                AliasType.Regex -> "`$from`"
                AliasType.Emoji -> "$from `$from`"
            }
        }
    }
}