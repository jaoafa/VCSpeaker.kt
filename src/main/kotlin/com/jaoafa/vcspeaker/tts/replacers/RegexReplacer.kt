package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake

/**
 * 正規表現エイリアスを置換するクラス
 */
object RegexReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Normal

    override suspend fun replace(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Regex) { alias, replacedText ->
            replacedText.replace(Regex(alias.search), alias.replace)
        }
}