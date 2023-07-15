package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.voicetext.Narrator.speakSelf
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import dev.kord.common.entity.Snowflake

class SpeakCommand : Extension() {
    override val name = "Speak"

    inner class SpeakOptions : Arguments() {
        val text by string {
            name = "text"
            description = "The text to speak."
        }
    }

    override suspend fun setup() {
        publicSlashCommand(::SpeakOptions) {
            name = "speak"
            description = "Speaks in the specified voice channel."

            guild(Snowflake(839462224505339954))

            action {
                VCSpeaker.guildPlayer[guild!!.id]?.speakSelf(arguments.text)
            }
        }
    }
}