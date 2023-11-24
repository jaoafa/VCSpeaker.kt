package com.jaoafa.vcspeaker.tts.replacers

import dev.kord.common.entity.Snowflake

/**
 * ロールメンションを置換するクラス
 */
object RoleMentionReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<@&(\\d+)>")) { kord, id ->
            kord.getGuildOrNull(guildId)?.getRole(id)?.data?.name ?: "不明なロール"
        }
}