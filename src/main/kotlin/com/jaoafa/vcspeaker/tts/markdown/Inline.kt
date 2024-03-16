package com.jaoafa.vcspeaker.tts.markdown

data class InlineMatch(val match: String, val text: String, val range: IntRange, val effect: InlineEffect)

data class Inline(val text: String, val effects: Set<InlineEffect>) {
    companion object {
        fun from(paragraph: String): List<Inline> {
            val allInlines = InlineEffect.entries.flatMap { effect ->
                effect.regex.findAll(paragraph).map { it to effect }
            }.map { (it, effect) ->
                val match = try {
                    it.groups["all"]!!.value
                } catch (e: Exception) {
                    it.value
                }

                val text = it.groups["text"]?.value ?: ""

                val range = try {
                    it.groups["all"]!!.range
                } catch (e: Exception) {
                    it.range
                }

                InlineMatch(match, text, range, effect)
            }.sortedBy { it.range.first }.toMutableList()

            val removedInlines = mutableListOf<InlineMatch>()

            // Remove non-effective inline effects
            for (testerMatch in listOf(*allInlines.toTypedArray())) { // Clone all inlines to test
                if (removedInlines.contains(testerMatch)) continue

                val range = testerMatch.range

                fun predicateRemove(match: InlineMatch) =
                    (range.contains(match.range.first) && !range.contains(match.range.last)) // Crossed each other
                            || (match.range == (range.first + 1) until range.last && match.text == testerMatch.text) // Remove redundant match

                removedInlines.addAll(allInlines.filter(::predicateRemove))
                allInlines.removeIf(::predicateRemove)
            }

            // Split paragraph into inlines
            val inlines = mutableListOf(Inline(paragraph, mutableSetOf()))

            for (inline in allInlines) {
                val targetInline = inlines.first { it.text.contains(inline.match) }
                val (beforeMatch, afterMatch) = targetInline.text.split(inline.match, limit = 2)

                val index = inlines.indexOf(targetInline)
                inlines.remove(targetInline)

                val effects = targetInline.effects

                inlines.addAll(index, listOf(
                    Inline(beforeMatch, effects),
                    Inline(inline.text, mutableSetOf(*effects.toTypedArray()).apply { add(inline.effect) }),
                    Inline(afterMatch, effects)
                ).filter { it.text.isNotEmpty() })
            }

            return inlines
        }
    }
}

enum class InlineEffect(val regex: Regex, val replacer: ((String) -> String)? = null) {
    Link(Regex("\\[(?<text>((?!https?://).)+?)]\\(<?(?<url>https?://.+?)>?\\)")),
    Code(Regex("`(?<text>.+?)`")),
    Bold(Regex("\\*\\*(?<text>.+?)\\*\\*")),
    Italic(Regex("(?=(?<all>(?<literal>[*_])(?<text>((?!\\k<literal>).)+?)\\k<literal>))")),
    Underline(Regex("__(?<text>.+?)__")),
    Strikethrough(Regex("~~(?<text>.+?)~~"), { "パー" }),
    Spoiler(Regex("\\|\\|(?<text>.+?)\\|\\|"), { "ピー" })
}