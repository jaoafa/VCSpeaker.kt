package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tts.Narrators.narrator
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension

class SpeakCommand : Extension() {
    override val name = this::class.simpleName!!

    inner class SpeakOptions : Options() {
        val text by string {
            name = "text"
            description = "読み上げる文章"
        }
    }

    override suspend fun setup() {
        publicSlashCommand("speak", "VCSpeaker として文章を読み上げます (デバッグ用)", ::SpeakOptions) {
            action {
                guild?.narrator()?.queueSelf(arguments.text)
                respond(arguments.text)
            }
        }
    }
}