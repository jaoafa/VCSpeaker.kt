package com.jaoafa.vcspeaker

object StringUtils {
    fun String.substringByCodePoints(start: Int, end: Int): String {
        val codePoints = codePoints().toArray()
        return String(codePoints.copyOfRange(start, end), 0, end - start)
    }

    fun String.lengthByCodePoints(): Long {
        return codePoints().count()
    }
}