package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.autoJoinEnabled
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.core.event.message.MessageCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging

class NewMessageEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

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

                    if (!targetChannel.isAfk())
                        targetChannel.join { event.message.respond(it) }
                } else if (!narratorActive) return@action

                val message = event.message

                if (message.content.startsWith(VCSpeaker.prefix)) return@action

                guild.narrator()?.scheduleAsUser(message)

                logger.info {
                    "[${guild.name}] Message Received: Adding the message by @${message.author?.username} to the queue."
                }
            }
        }
    }
}