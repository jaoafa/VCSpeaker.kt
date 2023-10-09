package com.jaoafa.vcspeaker.voicetext.textreplacers

import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake

object RegexReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake): String =
        replaceText(text, guildId, AliasType.Regex) { alias, replacedText ->
            replacedText.replace(Regex(alias.from), alias.to)
        }
}