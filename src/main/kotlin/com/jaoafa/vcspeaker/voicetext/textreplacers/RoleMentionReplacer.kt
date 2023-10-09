package com.jaoafa.vcspeaker.voicetext.textreplacers

import dev.kord.common.entity.Snowflake

object RoleMentionReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake): String =
        replaceMentionable(text, Regex("<@&(\\d+)>")) { kord, id ->
            kord.getGuildOrNull(guildId)?.getRole(id)?.data?.name ?: "不明なロール"
        }
}