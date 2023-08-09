package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.store.GuildStore
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.addReaction
import com.kotlindiscord.kord.extensions.utils.deleteOwnReaction
import dev.kord.core.event.message.MessageCreateEvent

class NewMessageEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check {
                anyGuild()
                isNotBot()
            }

            action {
                if (!GuildStore.getTextChannels().contains(event.message.channelId)) return@action
                if (!VCSpeaker.narrators.contains(event.guildId)) return@action

                val message = event.message
                message.addReaction("👀")

                val narrator = VCSpeaker.narrators[event.guildId]!! // checked in check

                narrator.queueUser(message.content, message.author!!.id, message) // Not bot

                message.deleteOwnReaction("👀")
            }
        }
    }
}