package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.core.entity.Message
import dev.kord.core.entity.effectiveName

class ReplyProcessor : BaseProcessor() {
    override val priority = 10

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val referencedMessage = message?.referencedMessage ?: return content to voice
        val replyToName = referencedMessage.author?.effectiveName ?: "だれか"
        return "$replyToName への返信、$content" to voice
    }
}