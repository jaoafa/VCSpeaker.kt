package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.StringUtils.lengthByCodePoints
import com.jaoafa.vcspeaker.StringUtils.substringByCodePoints
import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.core.entity.Message

class CharLimitProcessor : BaseProcessor() {
    override val priority = 110

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val limit = 180
        return content.let {
            if (it.lengthByCodePoints() > limit) it.substringByCodePoints(0, limit) else it
        } to voice
    }
}