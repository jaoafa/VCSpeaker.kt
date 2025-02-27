package com.jaoafa.vcspeaker.tts

data class TextToken(val text: String, val replacer: String? = null) {
    fun replaced() = replacer != null
}
