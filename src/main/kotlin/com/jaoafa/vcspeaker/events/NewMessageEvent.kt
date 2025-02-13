package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.autoJoinEnabled
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import dev.kord.common.entity.MessageType
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.respond
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

                val type = event.message.type

                if (type == MessageType.ChatInputCommand) return@action

                logger.info { "Message Received: $type" }

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
                    "[${guild.name}] Message Received: Adding the message by @${message.author?.username} to the queue.\n" +
                            "Message: ${message.content}"
                }
            }
        }
    }
}