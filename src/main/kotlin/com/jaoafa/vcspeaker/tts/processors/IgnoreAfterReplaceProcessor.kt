package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.stores.IgnoreType
import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message

class IgnoreAfterReplaceProcessor : BaseProcessor() {
    override val priority = 70

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val guildId = message?.getGuild()?.id ?: return content to voice
        if (content.shouldIgnoreOn(guildId)) cancel()
        return content to voice
    }


    private fun String.shouldIgnoreOn(guildId: Snowflake) =
        IgnoreStore.filter(guildId).any {
            when (it.type) {
                IgnoreType.Equals -> this == it.search
                IgnoreType.Contains -> contains(it.search)
            }
        }
}