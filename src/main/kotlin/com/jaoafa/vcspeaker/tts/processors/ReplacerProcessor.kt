package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tools.Emoji.replaceEmojisToName
import com.jaoafa.vcspeaker.tools.getClassesIn
import com.jaoafa.vcspeaker.tts.Token
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.replacers.BaseReplacer
import dev.kord.core.entity.Message

class ReplacerProcessor : BaseProcessor() {
    override val priority = 60

    val replacers = getClassesIn<BaseReplacer>("com.jaoafa.vcspeaker.tts.replacers")
        .mapNotNull {
            it.kotlin.objectInstance
        }.sortedByDescending { it.priority.level }

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val guildId = message?.getGuild()?.id ?: return content to voice
        val replacedText = replacers.fold(mutableListOf(Token(content))) { tokens, replacer ->
            replacer.replace(tokens, guildId)
        }.joinToString("") { it.text }.replaceEmojisToName()

        return replacedText to voice
    }
}