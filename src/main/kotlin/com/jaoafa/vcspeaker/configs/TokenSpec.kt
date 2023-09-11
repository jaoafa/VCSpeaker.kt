package com.jaoafa.vcspeaker.configs

import com.uchuhimo.konf.ConfigSpec

object TokenSpec : ConfigSpec() {

    /**
     * The Discord bot token.
     */
    val discord by required<String>()

    /**
     * The Discord bot token for development.
     */
    val discordDev by optional<String?>(null)

    /**
     * The VoiceText API token.
     */
    val voicetext by required<String>()

    /**
     * The VoiceText API token for development.
     */
    val voicetextDev by optional<String?>(null)
}