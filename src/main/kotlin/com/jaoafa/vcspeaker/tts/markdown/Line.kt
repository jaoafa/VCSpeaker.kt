package com.jaoafa.vcspeaker.tts.markdown

data class Line(val inlines: List<Inline>, val effects: Set<LineEffect>) {
    companion object {
        fun from(paragraph: String): Line {
            val prefixCandidates = paragraph.split(" ").filter { it.isNotEmpty() }

            val effects = mutableSetOf<LineEffect>()
            var skipped = false

            for (prefixCandidate in prefixCandidates) {
                if (skipped) break

                // null if this is not a prefix
                val prefix = LineEffect.entries.firstOrNull { it.regex.matches(prefixCandidate) }

                if (prefix != null) effects.add(prefix)
                else skipped = true
            }

            val text = prefixCandidates.drop(effects.size).joinToString(" ")

            val inlines = Inline.from(text)

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
    Heading1(Regex("^#$")),
    Heading2(Regex("^##$")),
    Heading3(Regex("^###$")),
    SmallHeading(Regex("^-#$")),
    Quote(Regex("^>$")),
    BulletList(Regex("^[*-]$")),
    NumberedList(Regex("^\\d+\\.$"))
}