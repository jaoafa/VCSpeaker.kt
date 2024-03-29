package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake

/**
 * 絵文字エイリアスを置換するクラス
 */
object EmojiReplacer : BaseReplacer {
    override val priority = ReplacerPriority.High

    override suspend fun replace(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Emoji) { alias, replacedText ->
            replacedText.replace(alias.search, alias.replace)
        }
}