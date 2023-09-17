package com.jaoafa.vcspeaker.tools

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

object Emoji {
    private var codePoints = runBlocking {
        val client = HttpClient(CIO)

        val response = client.get("https://unicode.org/Public/emoji/latest/emoji-test.txt")
        val lines = response.bodyAsText().lines().filter { !it.startsWith("#") && it.isNotEmpty() }

        lines.map { it.split(";").first().trim().split(" ") }
    }

    fun String.startsWithEmoji(): Boolean {
        return codePoints.any { this.hexCodePoints().startsWith(it) }
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