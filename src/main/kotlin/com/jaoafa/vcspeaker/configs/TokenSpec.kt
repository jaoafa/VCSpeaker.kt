package com.jaoafa.vcspeaker.configs

import com.uchuhimo.konf.ConfigSpec

object TokenSpec : ConfigSpec() {

    /**
     * The Discord bot token.
     */
    val discord by required<String>()

    /**
     * The VoiceText API token.
     */
    val voicetext by required<String>()
}