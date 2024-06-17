package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.core.entity.Message

class StickerProcessor : BaseProcessor() {
    override val priority = 20

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val stickers = message?.stickers ?: return content to voice
        if (stickers.isEmpty()) return content to voice

        return (content + " " + stickers.joinToString(" ") { "スタンプ ${it.name}" }).trim() to voice
    }
}