package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.features.Ignore.shouldIgnoreOn
import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.core.entity.Message

class IgnoreAfterReplaceProcessor : BaseProcessor() {
    override val priority = 80

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val guildId = message?.getGuild()?.id ?: return content to voice
        if (content.shouldIgnoreOn(guildId)) cancel()
        return content to voice
    }
}