package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tts.Token
import dev.kord.common.entity.Snowflake

/**
 * エイリアスを置換するクラス
 */
object AliasReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Low

    override suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake) =
        replaceText(tokens, guildId, AliasType.Text) { alias, replacedTokens ->
            buildList {
                for (replacedToken in replacedTokens) {
                    val text = replacedToken.text

                    if (replacedToken.replaced || !text.contains(alias.search)) {
                        add(replacedToken)
                        continue
                    }

                    val splitTexts = text.split(alias.search)

                    val additions = splitTexts.mixin {
                        Token(alias.replace, true)
                    }

                    addAll(additions)
                }
            }.toMutableList()
        }
}