package com.jaoafa.vcspeaker.tts.markdown

fun String.removeCodeBlock(): String {
    val texts = split("```")
    val plainTextCandidates = texts.filterIndexed { index, _ -> index % 2 == 0 }
    return plainTextCandidates.joinToString("").let {
        if (texts.size % 2 == 0) it + "```" + texts.last()
        else it
    }.lines().filter { it.isNotEmpty() }.joinToString("\n")
}

fun String.toMarkdown() = removeCodeBlock().lines().map { Line.from(it) }