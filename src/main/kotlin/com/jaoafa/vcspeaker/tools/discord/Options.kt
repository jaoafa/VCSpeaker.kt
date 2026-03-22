package com.jaoafa.vcspeaker.tools.discord

import dev.kordex.core.commands.Arguments

open class Options : Arguments() {
    override fun toString(): String {
        return super.args.associate { it.displayName to it.converter.parsed }.toString()
    }
}

interface VoiceOptions {
    val speaker: String?
    val emotion: String?
    val emotionLevel: Int?
    val pitch: Int?
    val speed: Int?
    val volume: Int?
}