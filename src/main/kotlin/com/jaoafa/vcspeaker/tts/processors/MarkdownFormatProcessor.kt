package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.markdown.toMarkdown
import dev.kord.core.entity.Message

class MarkdownFormatProcessor : BaseProcessor() {
    override val priority = 60

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val markdown = content.toMarkdown().joinToString("") { it.toReadable() }

        return markdown to voice
    }
}