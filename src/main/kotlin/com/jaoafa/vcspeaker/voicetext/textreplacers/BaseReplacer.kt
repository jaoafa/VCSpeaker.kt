package com.jaoafa.vcspeaker.voicetext.textreplacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.AliasData
import com.jaoafa.vcspeaker.stores.AliasStore
import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord

/**
 * テキストを置換する基底クラス
 */
interface BaseReplacer {
    suspend fun replace(text: String, guildId: Snowflake): String

    fun replaceText(
        text: String,
        guildId: Snowflake,
        type: AliasType,
        transform: (AliasData, String) -> String
    ): String {
        val aliases = AliasStore.filter(guildId).filter { it.type == type }

        val replacedText = aliases.fold(text) { replacedText, alias ->
            transform(alias, replacedText)
        }

        return replacedText
    }

    suspend fun replaceMentionable(
        text: String,
        regex: Regex,
        nameSupplier: suspend (Kord, Snowflake) -> String
    ): String {
        val matches = regex.findAll(text)

        val replacedText = matches.fold(text) { replacedText, match ->
            val id = Snowflake(match.groupValues[1]) // 0 is for whole match
            val name = nameSupplier(VCSpeaker.kord, id)

            replacedText.replace(match.value, name)
        }

        return replacedText
    }
}