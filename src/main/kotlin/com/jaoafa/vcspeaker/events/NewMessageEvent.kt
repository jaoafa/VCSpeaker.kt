package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.autoJoinEnabled
import com.jaoafa.vcspeaker.voicetext.Narrators.narrator
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
                if (event.message.content.startsWith(VCSpeaker.prefix)) return@action

                val guild = event.getGuildOrNull() ?: return@action

                val narratorActive = guild.narrator() != null
                val autoJoin = event.getGuildOrNull()!!.autoJoinEnabled() // checked

                if (!narratorActive && autoJoin) {
                    val targetChannel =
                        event.member!!.getVoiceStateOrNull()?.getChannelOrNull() ?: return@action

                    targetChannel.join(message = event.message)
                } else if (!narratorActive) return@action

                val message = event.message

                if (message.content.startsWith(VCSpeaker.prefix)) return@action

                message.addReaction("👀")

                guild.narrator()?.queueUser(message) // Not bot

                message.deleteOwnReaction("👀")
            }
        }
    }
}