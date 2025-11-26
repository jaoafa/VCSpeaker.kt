package com.jaoafa.vcspeaker.tools

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

data class EmojiData(
    val unicode: String,
    val name: String
)

object Emoji {
    private val logger = KotlinLogging.logger {}

    private var emojis = runBlocking {
        logger.info { "Loading emojis..." }

        val client = HttpClient(CIO) {
            VCSpeakerUserAgent()
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000 // 5 minutes
            }
        }

        val response = client.get("https://gist.githubusercontent.com/yuuahp/64fa9c60a198f71d211629906079d040/raw/f3429f8ee26776d9028f8cfac5cc0608efc83ce0/emoji-test.txt")
        val lines = response.bodyAsText().lines().filter { !it.startsWith("#") && it.isNotEmpty() }

        val emojiDataString = lines.map { it.split("#").last().trim() }
        val emojiData = emojiDataString.map {
            val stringPart = it.split(" ")
            val unicode = stringPart.first()
            val name = stringPart.drop(2).joinToString(" ")

            EmojiData(unicode, name)
        }.filter { it.unicode.isNotEmpty() && it.name.isNotEmpty() }

        logger.info { "Loading emojis complete." }

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

    fun String.removeEmojis() = emojis.fold(this) { replaced, emoji ->
        replaced.replace(emoji.unicode, "")
    }

    fun String.replaceEmojisToName() = emojis.fold(this) { replaced, emoji ->
        replaced.replace(emoji.unicode, emoji.name)
    }

    fun String.containsEmojis() = emojis.any { contains(it.unicode) }

    fun String.getEmojis() = emojis.filter { contains(it.unicode) }

    private fun List<String>.startsWith(list: List<String>): Boolean {
        if (this.size < list.size) return false

        for (i in list.indices)
            if (this[i] != list[i]) return false

        return true
    }
}