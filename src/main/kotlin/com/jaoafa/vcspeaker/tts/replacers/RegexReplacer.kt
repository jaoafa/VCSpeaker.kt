package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tts.Token
import dev.kord.common.entity.Snowflake

/**
 * 正規表現エイリアスを置換するクラス
 */
object RegexReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Low

    override suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake) =
        replaceText(tokens, guildId, AliasType.Regex) { alias, replacedTokens ->
            buildList {
                val regex = Regex(alias.search)

                for (replacedToken in replacedTokens) {
                    val text = replacedToken.text

                    if (replacedToken.replaced() || !text.partialMatch(regex)) {
                        add(replacedToken)
                        continue
                    }

                    val splitTexts = text.split(regex)

                    val additions = splitTexts.mixin {
                        Token(alias.replace, "Regex Alias `${alias.search}` →「${alias.replace}」")
                    }

                    addAll(additions)
                }
            }.toMutableList()
        }
}