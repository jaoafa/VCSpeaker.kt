package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension

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
            check { anyGuild() }
            action {
                guild?.narrator()?.scheduleAsSystem(arguments.text)
                respond(arguments.text)
            }
        }
    }
}