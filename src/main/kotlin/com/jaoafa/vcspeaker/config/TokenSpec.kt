package com.jaoafa.vcspeaker.config

import com.uchuhimo.konf.ConfigSpec

object TokenSpec : ConfigSpec() {
    val discord by required<String>()
    val discordDev by required<String>()
    val voicetext by required<String>()
}