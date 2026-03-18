package com.jaoafa.vcspeaker.tools.discord

import dev.kordex.core.commands.Arguments

open class Options : Arguments() {
    override fun toString(): String {
        return super.args.associate { it.displayName to it.converter.parsed }.toString()
    }
}