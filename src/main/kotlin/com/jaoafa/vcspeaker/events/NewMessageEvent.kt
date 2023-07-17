package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.store.GuildStore
import com.jaoafa.vcspeaker.voicetext.Speaker
import com.jaoafa.vcspeaker.voicetext.Voice
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.addReaction
import com.kotlindiscord.kord.extensions.utils.deleteOwnReaction
import dev.kord.core.event.message.MessageCreateEvent

class NewMessageEvent : Extension() {
    override val name = "NewMessageEvent"

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check {
                anyGuild()
                isNotBot()
                assert(GuildStore.getTextChannels().contains(event.message.channelId)) // In target text channel
                assert(VCSpeaker.narrators.contains(event.guildId)) // In VC
            }

            action {
                val message = event.message
                message.addReaction("👀")

                val narrator = VCSpeaker.narrators[event.guildId]!!

                narrator.queue(message.content, Voice(speaker = Speaker.Hikari)) // Not bot

                message.deleteOwnReaction("👀")
            }
        }
    }
}