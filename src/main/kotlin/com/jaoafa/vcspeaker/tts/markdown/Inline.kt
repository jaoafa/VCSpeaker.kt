package com.jaoafa.vcspeaker.tts.markdown

enum class InlineEffect {
    Code, Bold, Underline, Italic, Strikethrough, Spoiler,
}

val markers = mapOf(
    "`" to InlineEffect.Code,
    "**" to InlineEffect.Bold,
    "__" to InlineEffect.Underline,
    "*" to InlineEffect.Italic,
    "_" to InlineEffect.Italic,
    "~~" to InlineEffect.Strikethrough,
    "||" to InlineEffect.Spoiler
)

data class Inline(val text: String, val effects: MutableSet<InlineEffect>) {
    companion object {
        private val linkRegex = Regex("\\[(?<text>((?!https?://).)+?)]\\(<?(?<url>https?://.+?)>?\\)")

        fun from(paragraph: String): List<Inline> {
            val linkRemoved = paragraph.replace(linkRegex) {
                it.groups["text"]?.value ?: it.value
            }

            val inlines = mutableListOf<Inline>()
            val effects = mutableMapOf<InlineEffect, Int>()
            var stack = ""
            var startEffectStack = ""
            var closeEffectStack = ""
            var closeMayFinish = false // some text **bold text** s <- here

            fun processEffect() {
                val closedEffects = mutableListOf<InlineEffect>()

                for ((marker, effect) in markers) {
                    if (startEffectStack.endsWith(marker) && closeEffectStack.startsWith(marker)) {
                        startEffectStack = startEffectStack.removeSuffix(marker)
                        closeEffectStack = closeEffectStack.removePrefix(marker)

                        closedEffects.add(effect)
                    }
                }

                inlines.add(Inline(stack + closeEffectStack, closedEffects.toMutableSet()))

                for (closedEffect in closedEffects) {
                    val startIndex = effects[closedEffect] ?: continue

                    inlines.forEachIndexed { i, inline ->
                        if (startIndex <= i) {
                            inline.effects += closedEffect
                        }
                    }
                }

                closedEffects.forEach { effects.remove(it) }
                stack = ""
                closeEffectStack = ""
            }

            linkRemoved.forEach { char -> // loop through each character
                if (markers.keys.joinToString("").contains(char)) { // marker
                    if (stack.isNotEmpty() && startEffectStack.isNotEmpty()) { // closing marker
                        closeMayFinish = true
                        closeEffectStack += char
                    } else { // starting marker
                        if (stack.isNotEmpty()) { // text without effects
                            inlines.add(Inline(stack, effects.keys.toMutableSet()))
                            effects.clear()
                            stack = ""
                        }

                        startEffectStack += char
                    }
                } else { // text
                    if (closeMayFinish) { // close effect finished
                        processEffect()
                        closeMayFinish = false
                    } else if (stack.isEmpty() && startEffectStack.isNotEmpty()) { // start effect finished
                        var effectStack = startEffectStack

                        for ((marker, effect) in markers) {
                            if (effectStack.endsWith(marker)) {
                                effects[effect] = inlines.size
                                effectStack = effectStack.removeSuffix(marker)
                            }
                        }

                        stack += effectStack // ineffective marker
                    }

                    stack += char
                }
            }

            // process remaining text
            if (stack.isNotEmpty()) processEffect()

            return inlines
        }
    }
}
