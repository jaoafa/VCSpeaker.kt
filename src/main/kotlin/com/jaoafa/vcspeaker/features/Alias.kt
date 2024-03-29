package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.stores.AliasStore
import com.jaoafa.vcspeaker.stores.AliasType
import com.kotlindiscord.kord.extensions.utils.FilterStrategy
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.core.entity.interaction.AutoCompleteInteraction
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder

object Alias {
    val autocomplete: suspend AutoCompleteInteraction.(AutoCompleteInteractionCreateEvent) -> Unit = { event ->
        val guildId = event.interaction.getChannel().data.guildId.value

        suggestStringMap(
            AliasStore.data.filter { it.guildId == guildId }
                .associate { "${it.type.displayName} / ${it.search} → ${it.replace}" to it.search },
            FilterStrategy.Contains
        )
    }

    fun EmbedBuilder.fieldAliasFrom(type: AliasType, from: String) =
        this.field("${type.emoji} ${type.displayName}", true) {
            when (type) {
                AliasType.Text -> from
                AliasType.Regex -> "`$from`"
                AliasType.Emoji -> "$from `$from`"
            }
        }
}