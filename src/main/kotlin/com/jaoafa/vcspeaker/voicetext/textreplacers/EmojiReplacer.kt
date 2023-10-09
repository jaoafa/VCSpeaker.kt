package com.jaoafa.vcspeaker.voicetext.textreplacers

import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake

/**
 * 絵文字エイリアスを置換するクラス
 */
object EmojiReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Emoji) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }
}