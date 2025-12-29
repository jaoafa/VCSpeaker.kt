package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tts.TextToken
import dev.kord.common.entity.Snowflake

/**
 * エイリアスを置換するクラス
 */
object AliasReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Low

    override suspend fun replace(tokens: MutableList<TextToken>, guildId: Snowflake) =
        replaceText(tokens, guildId, AliasType.Text) { alias, replacedTokens ->
            buildList {
                for (replacedToken in replacedTokens) {
                    val text = replacedToken.text

                    // should be skipped
                    if (replacedToken.replaced() || !text.contains(alias.search)) {
                        add(replacedToken)
                        continue
                    }

                    val splitTexts = text.split(alias.search)

                    val additions = splitTexts.mixin {
                        if (alias.soundboard != null) {
                            // Replace with soundboard tag
                            TextToken("<sound:0:${alias.soundboard}>", "Text Alias「${alias.search}」→ 🔊 Soundboard ${alias.soundboard}")
                        } else {
                            // Normal text replacement
                            TextToken(alias.replace, "Text Alias「${alias.search}」→「${alias.replace}」")
                        }
                    }

                    addAll(additions)
                }
            }.toMutableList()
        }
}