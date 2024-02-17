package com.jaoafa.vcspeaker.tts.replacers

import dev.kord.common.entity.Snowflake

object SpoilerReplacer : BaseReplacer {
    override val priority = ReplacerPriority.High

    override suspend fun replace(text: String, guildId: Snowflake) = text.replace(
        Regex("\\|\\|.*?\\|\\|"),
        "ピー"
    )
}