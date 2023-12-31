package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake

/**
 * エイリアスを置換するクラス
 */
object AliasReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Normal

    override suspend fun replace(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Text) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }
}