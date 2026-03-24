package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.database.tables.IgnoreEntity
import com.jaoafa.vcspeaker.database.tables.IgnoreTable
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.interaction.AutoCompleteInteraction
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kordex.core.utils.FilterStrategy
import dev.kordex.core.utils.suggestIntMap
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Ignore {
    val autocomplete: suspend AutoCompleteInteraction.(AutoCompleteInteractionCreateEvent) -> Unit =
        autocomplete@{ event ->
            val guildId = event.interaction.getChannel().data.guildId.value ?: return@autocomplete

            val stringMap = transaction {
                IgnoreEntity
                    .find { IgnoreTable.guildDid eq guildId }
                    .associate { "${it.type.displayName} / ${it.search}" to it.id.value }
            }

            suggestIntMap(stringMap, FilterStrategy.Contains)
        }

    fun getIgnoresOf(guildId: Snowflake) = transaction {
        IgnoreEntity
            .find { IgnoreTable.guildDid eq guildId }
            .sortedBy { it.search.length }
            .map { it.getRow() }
    }

    fun getEffectiveIgnoresOf(text: String, guildId: Snowflake) = getIgnoresOf(guildId).filter { it.match(text) }

    fun shouldBeIgnored(text: String, guildId: Snowflake) = getIgnoresOf(guildId).any { it.match(text) }
}