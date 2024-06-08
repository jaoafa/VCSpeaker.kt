package com.jaoafa.vcspeaker.tts

data class Token(val text: String, val replacer: String? = null) {
    fun replaced() = replacer != null
}
