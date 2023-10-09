package com.jaoafa.vcspeaker.voicetext.textreplacers

import com.jaoafa.vcspeaker.stores.AliasType
import dev.kord.common.entity.Snowflake

object AliasReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake): String =
        replaceText(text, guildId, AliasType.Text) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }
}