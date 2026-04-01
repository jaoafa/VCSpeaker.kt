package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.database.tables.AliasEntity
import com.jaoafa.vcspeaker.database.tables.AliasTable
import dev.kord.core.entity.interaction.AutoCompleteInteraction
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kordex.core.utils.FilterStrategy
import dev.kordex.core.utils.suggestIntMap
import kotlinx.serialization.Serializable
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

@Serializable
enum class AliasType(
    val displayName: String,
    val emoji: String
) {
    Text("文字列", ":pencil:"),
    Regex("正規表現", ":asterisk:"),
    Emoji("絵文字", ":neutral_face:"),
    Soundboard("サウンドボード", ":sound:")
}
