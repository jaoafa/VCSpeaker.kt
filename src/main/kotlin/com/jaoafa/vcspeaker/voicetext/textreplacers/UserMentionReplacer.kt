package com.jaoafa.vcspeaker.voicetext.textreplacers

import dev.kord.common.entity.Snowflake

object UserMentionReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake): String =
        replaceMentionable(text, Regex("<@!?(\\d+)>")) { kord, id ->
            val displayName = kord.getGuildOrNull(guildId)?.getMember(id)?.displayName
            displayName ?: "不明なユーザー"
        }
}