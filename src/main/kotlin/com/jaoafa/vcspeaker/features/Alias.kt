package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.database.tables.AliasEntity
import com.jaoafa.vcspeaker.database.tables.AliasTable
import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.core.entity.interaction.AutoCompleteInteraction
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kordex.core.utils.FilterStrategy
import dev.kordex.core.utils.suggestIntMap
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Alias {
    val autocomplete: suspend AutoCompleteInteraction.(AutoCompleteInteractionCreateEvent) -> Unit =
        autocomplete@{ event ->
            val guildId = event.interaction.getChannel().data.guildId.value ?: return@autocomplete

            val stringMap = transaction {
                AliasEntity
                    .find { AliasTable.guildDid eq guildId }
                    .associate { "${it.type.displayName} / ${it.search} → ${it.replace}" to it.id.value }
            }

            suggestIntMap(stringMap, FilterStrategy.Contains)
        }

    fun EmbedBuilder.fieldAliasFrom(type: AliasType, search: String) =
        this.field("${type.emoji} ${type.displayName}", true) {
            when (type) {
                AliasType.Text -> search
                AliasType.Regex -> "`$search`"
                AliasType.Emoji -> "$search `$search`"
                AliasType.Soundboard -> search
            }
        }
}
