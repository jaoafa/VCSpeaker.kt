package com.jaoafa.vcspeaker.tts.markdown

data class InlineMatch(val match: String, val text: String, val range: IntRange, val effect: InlineEffect)

data class Inline(val text: String, val effects: MutableSet<InlineEffect>) {
    val linkRegex = Regex("\\[(?<text>((?!https?://).)+?)]\\(<?(?<url>https?://.+?)>?\\)")

    companion object {
        fun from(paragraph: String): List<Inline> {
            //todo remove links

            val inlines = mutableListOf<Inline>()
            val effects = mutableMapOf<InlineEffect, Int>()
            var stack = ""
            var startEffectStack = ""
            var closeEffectStack = ""
            var closeFinish = false // some text **bold text** s <- here

            paragraph.forEach { char ->
                if (markers.keys.joinToString("").contains(char)) { // marker
                    if (stack.isNotEmpty() && startEffectStack.isNotEmpty()) { // closing marker
                        closeFinish = true
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
                    if (closeFinish) {
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

                        closedEffects.forEach { effects.remove(it) != null }
                        stack = ""
                        closeEffectStack = ""
                        closeFinish = false
                    } else if (stack.isEmpty() && startEffectStack.isNotEmpty()) { // start effect finished
                        for ((marker, effect) in markers) {
                            if (startEffectStack.endsWith(marker)) {
                                effects[effect] = inlines.size
                            }
                        }
                    }

                    stack += char
                }
            }

            if (stack.isNotEmpty()) {
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
                            inline.effects.add(closedEffect)
                        }
                    }
                }

                closedEffects.forEach { effects.remove(it) }
                stack = ""
                closeEffectStack = ""
            }

            return inlines
        }
    }
}

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