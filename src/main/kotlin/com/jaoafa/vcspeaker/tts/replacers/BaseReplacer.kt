package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.AliasData
import com.jaoafa.vcspeaker.stores.AliasStore
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tts.Token
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.coroutines.runBlocking

/**
 * テキストを置換する基底クラス
 */
interface BaseReplacer {
    val priority: ReplacerPriority

    suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake): MutableList<Token>

    fun replaceText(
        tokens: MutableList<Token>,
        guildId: Snowflake,
        type: AliasType,
        transform: (AliasData, MutableList<Token>) -> MutableList<Token>
    ): MutableList<Token> {
        val aliases = AliasStore.filter(guildId).filter { it.type == type }

        val replacedText = aliases.fold(tokens) { replacedTokens, alias ->
            transform(alias, replacedTokens)
        }

        return replacedText
    }

    suspend fun replaceMentionable(
        tokens: MutableList<Token>,
        regex: Regex,
        nameSupplier: suspend (Kord, Snowflake) -> String
    ): MutableList<Token> {
        val newTokens = mutableListOf<Token>()

        for (token in tokens) {
            val text = token.text

            if (token.replaced || !text.partialMatch(regex)) {
                newTokens.add(token)
                continue
            }

            val matches = regex.findAll(text).toList()

            val splitTexts = text.split(regex)

            val additions = splitTexts.mixin { index ->
                val match = matches[index]
                val id = Snowflake(match.groupValues[1]) // 0 is for whole match
                val name = nameSupplier(VCSpeaker.kord, id)

                Token(name, true)
            }

            newTokens.addAll(additions)
        }

        return newTokens
    }


    fun List<String>.mixin(provider: suspend (Int) -> Token) = buildList {
        // ["Token1", "Token2", "Token3"] -> ["Token1", provider(1), "Token2", provider(2), "Token3"]
        for (index in 0..(this@mixin.size * 2 - 2)) {
            if (index % 2 == 0) add(Token(this@mixin[index / 2]))
            else add(runBlocking { provider((index - 1) / 2) })
        }
    }

    fun String.partialMatch(regex: Regex) = regex.findAll(this).toList().isNotEmpty()
}