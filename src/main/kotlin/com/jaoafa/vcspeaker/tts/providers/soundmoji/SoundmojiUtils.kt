package com.jaoafa.vcspeaker.tts.providers.soundmoji

object SoundmojiUtils {
    private val soundTagRegex = Regex("<sound:\\d+:(\\d+)>")
    private val cdnUrlRegex = Regex(
        "https?://cdn\\.discordapp\\.com/soundboard-sounds/(\\d+)(?:\\.[a-zA-Z0-9]+)?(?:\\?[^\\s<>]+)?",
        RegexOption.IGNORE_CASE
    )
    private val rawIdRegex = Regex("^\\s*(\\d+)\\s*$")

    fun containsSoundmojiReference(text: String): Boolean = extractSoundmojiIds(text).isNotEmpty()

    fun extractSoundmojiIds(text: String): List<Long> = buildList {
        soundTagRegex.findAll(text).forEach { match ->
            match.groupValues[1].toLongOrNull()?.let { add(it) }
        }
        cdnUrlRegex.findAll(text).forEach { match ->
            match.groupValues[1].toLongOrNull()?.let { add(it) }
        }
        rawIdRegex.matchEntire(text)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { add(it) }
    }

    fun normalizeSoundmojiReferences(text: String): String {
        val rawMatch = rawIdRegex.matchEntire(text)
        if (rawMatch != null) {
            val id = rawMatch.groupValues[1].toLongOrNull()
            if (id != null) return "<sound:0:$id>"
        }

        return cdnUrlRegex.replace(text) { match ->
            "<sound:0:${match.groupValues[1]}>"
        }
    }
}
