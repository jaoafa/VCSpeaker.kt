package com.jaoafa.vcspeaker.tts.markdown

data class Line(val inlines: List<Inline>, val effects: Set<LineEffect>) {
    companion object {
        fun from(paragraph: String): Line {
            val inlines = Inline.from(paragraph)
            val plainText = inlines.joinToString("") { it.text }
            val prefixCandidates = plainText.split(" ").filter { it.isNotEmpty() }

            val effects = mutableSetOf<LineEffect>()
            var skipped = false

            for (prefixCandidate in prefixCandidates) {
                // null if this is not a prefix
                val prefix = LineEffect.entries.firstOrNull { it.regex.matches(prefixCandidate) }

                if (prefix != null && !skipped) effects.add(prefix)
                else skipped = true
            }

            return Line(inlines, effects)
        }
    }

    fun toReadable() = inlines.joinToString("") {
        it.effects.fold(it.text) { text, effect ->
            effect.replacer?.invoke(text) ?: text
        }
    }
}

enum class LineEffect(val regex: Regex) {
    Header(Regex("^#{1,3}$")),
    Quote(Regex("^>$")),
    BulletList(Regex("^[*-]$")),
    NumberedList(Regex("^\\d+\\.$"))
}