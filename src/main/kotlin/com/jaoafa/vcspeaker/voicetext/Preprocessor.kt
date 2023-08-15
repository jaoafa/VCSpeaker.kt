package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.store.AliasData
import com.jaoafa.vcspeaker.store.AliasStore
import com.jaoafa.vcspeaker.store.AliasType
import com.jaoafa.vcspeaker.store.IgnoreStore
import dev.kord.common.entity.Snowflake

// ignore -> emoji -> regex -> text

object Preprocessor {
    fun processText(guildId: Snowflake, text: String): String? {
        if (shouldIgnore(guildId, text)) return null

        var replacedText = replaceEmoji(guildId, text)
        replacedText = replaceRegex(guildId, replacedText)
        replacedText = replaceText(guildId, replacedText)

        return replacedText
    }

    private fun shouldIgnore(guildId: Snowflake, text: String) =
        IgnoreStore.filter(guildId).any {
            text.contains(it.text)
        }

    private fun replaceEmoji(guildId: Snowflake, text: String) =
        replace(guildId, text, AliasType.Emoji) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }

    private fun replaceRegex(guildId: Snowflake, text: String) =
        replace(guildId, text, AliasType.Regex) { alias, replacedText ->
            replacedText.replace(Regex(alias.from), alias.to)
        }

    private fun replaceText(guildId: Snowflake, text: String) =
        replace(guildId, text, AliasType.Text) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }

    private fun replace(
        guildId: Snowflake,
        text: String,
        type: AliasType,
        transform: (AliasData, String) -> String
    ): String {
        val aliases = AliasStore.filter(guildId).filter { it.type == type }

        var replacedText = text

        for (alias in aliases)
            replacedText = transform(alias, replacedText)

        return replacedText
    }
}