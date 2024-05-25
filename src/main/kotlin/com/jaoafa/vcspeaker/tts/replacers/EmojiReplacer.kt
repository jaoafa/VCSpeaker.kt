package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tts.Token
import dev.kord.common.entity.Snowflake

/**
 * 絵文字エイリアスを置換するクラス
 */
object EmojiReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Normal

    override suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake) =
        replaceText(tokens, guildId, AliasType.Emoji) { alias, replacedTokens ->
            buildList {
                for (token in replacedTokens) {
                    val text = token.text

                    if (token.replaced || !text.contains(alias.search)) {
                        add(token)
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