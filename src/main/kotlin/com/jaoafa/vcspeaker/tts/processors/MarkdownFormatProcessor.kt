package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.markdown.InlineEffect
import com.jaoafa.vcspeaker.tts.markdown.LineEffect
import com.jaoafa.vcspeaker.tts.markdown.toMarkdown
import dev.kord.core.entity.Message

class MarkdownFormatProcessor : BaseProcessor() {
    override val priority = 80

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val markdown = content.toMarkdown()

        val heading = markdown.first().effects.firstOrNull { it.name.startsWith("Heading") }

        val newVoice = if (markdown.size == 1 && heading != null) {
            when (heading) {
                LineEffect.Heading1 -> voice.copy(speed = 200)
                LineEffect.Heading2 -> voice.copy(speed = 175)
                LineEffect.Heading3 -> voice.copy(speed = 150)
                else -> voice
            }
        } else voice

        val readableMarkdown = markdown.joinToString(" ") { line ->
            line.toReadable {
                if (it.effects.contains(InlineEffect.Spoiler)) {
                    "ピー"
                } else if (it.effects.contains(InlineEffect.Strikethrough)) {
                    "パー"
                } else {
                    it.text
                }
            }
        }

        return readableMarkdown to newVoice
    }
}