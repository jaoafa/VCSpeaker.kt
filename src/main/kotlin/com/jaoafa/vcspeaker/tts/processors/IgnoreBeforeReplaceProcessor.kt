package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.features.Ignore.shouldIgnoreOn
import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.core.entity.Message

class IgnoreBeforeReplaceProcessor : BaseProcessor() {
    override val priority = 70

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val guildId = message?.getGuild()?.id ?: return content to voice
        if (content.shouldIgnoreOn(guildId)) cancel()
        return content to voice
    }
}