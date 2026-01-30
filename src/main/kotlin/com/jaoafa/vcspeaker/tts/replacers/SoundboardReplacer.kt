package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tts.TextToken
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiUtils
import dev.kord.common.entity.Snowflake

/**
 * サウンドボードエイリアスを置換するクラス
 */
object SoundboardReplacer : BaseReplacer {
    override val priority = ReplacerPriority.High

    override suspend fun replace(tokens: MutableList<TextToken>, guildId: Snowflake) =
        replaceText(tokens, guildId, AliasType.Soundboard) { alias, replacedTokens ->
            buildList {
                val normalized = SoundmojiUtils.normalizeSoundmojiReferences(alias.replace)

                for (replacedToken in replacedTokens) {
                    val text = replacedToken.text

                    if (replacedToken.replaced() || !text.contains(alias.search)) {
                        add(replacedToken)
                        continue
                    }

                    val splitTexts = text.split(alias.search)

                    val additions = splitTexts.mixin {
                        TextToken(normalized, "Soundboard Alias「${alias.search}」→「$normalized」")
                    }

                    addAll(additions)
                }
            }.toMutableList()
        }
}
