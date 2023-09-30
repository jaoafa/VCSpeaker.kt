package com.jaoafa.vcspeaker.tools

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

data class EmojiData(
    val unicode: String,
    val name: String
)

object Emoji {
    private var emojis = runBlocking {
        println("Loading emoji data...")

        val client = HttpClient(CIO)

        val response = client.get("https://unicode.org/Public/emoji/latest/emoji-test.txt")
        val lines = response.bodyAsText().lines().filter { !it.startsWith("#") && it.isNotEmpty() }

        val emojiDataString = lines.map { it.split("#").last().trim() }
        val emojiData = emojiDataString.map {
            val stringPart = it.split(" ")
            val emoji = stringPart.first()
            val name = stringPart.drop(2).joinToString(" ")

            EmojiData(emoji, name)
        }

        println("Loaded emoji data.")

        emojiData
    }

    fun String.startsWithEmoji(): Boolean {
        return emojis.any { this.startsWith(it.unicode) }
    }

    fun String.removeFirstEmoji(): String {
        return removePrefix(getFirstEmoji().unicode)
    }

    fun String.getFirstEmoji(): EmojiData {
        return emojis.filter { startsWith(it.unicode) }.maxByOrNull { it.unicode.length }!!
    }

    fun String.replaceEmojiToName(): String {
        return emojis.fold(this) { replaced, emoji ->
            replaced.replace(emoji.unicode, emoji.name)
        }
    }

    private fun List<String>.startsWith(list: List<String>): Boolean {
        if (this.size < list.size) return false

        for (i in list.indices)
            if (this[i] != list[i]) return false

        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun String.hexCodePoints() = buildList {
        repeat(codePointCount(0, length)) {
            val i = offsetByCodePoints(0, it)
            val codePoint = codePointAt(i)
            var hexString = codePoint.toHexString(HexFormat.UpperCase)

            while (hexString.startsWith("0")) hexString = hexString.removePrefix("0")

            add(hexString)
        }
    }
}