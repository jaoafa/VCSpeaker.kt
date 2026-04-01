package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.actions.GuildAction.fetchSnapshot
import com.jaoafa.vcspeaker.database.actions.ReadableBotAction.isReadableBotOn
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.tools.discord.anyGuildRegistered
import com.jaoafa.vcspeaker.tts.narrators.NarratorManager.getNarrator
import dev.kord.common.entity.MessageType
import dev.kord.core.event.message.MessageCreateEvent
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
                anyGuildRegistered()
                failIf {
                    val message = event.message
                    val guild = event.getGuildOrNull() ?: return@failIf true
                    val author = message.author ?: return@failIf true

                    // 人間なら弾かない
                    if (!author.isBot) return@failIf false

                    // 読み上げ可能Botでない場合は弾く
                    !author.isReadableBotOn(guild)
                }
            }

            action {
                val guild = event.getGuildOrNull() ?: return@action
                val message = event.message
                val guildSnapshot = guild.fetchSnapshot()

                // Not the voice-text channel
                if (guildSnapshot.channelDid != message.channelId) return@action
                // The message starts with the configured prefix
                if (message.content.startsWith(guildSnapshot.prefix ?: VCSpeaker.prefix)) return@action
                // The message is a slash command
                if (message.type == MessageType.ChatInputCommand) return@action

                val isNarratorActive = guild.getNarrator() != null

                // No narrator is active and autojoin is enabled
                if (!isNarratorActive && guildSnapshot.autoJoin) {
                    val targetChannel = event.member?.getVoiceStateOrNull()?.getChannelOrNull() ?: return@action

                    if (!targetChannel.isAfk())
                        targetChannel.join { event.message.respond(it) }
                } else if (!isNarratorActive) return@action

                guild.getNarrator()?.scheduleAsUser(message)

                logger.info {
                    "[${guild.name}] Message Received: Adding the message by @${message.author?.username} to the queue."
                }
            }
        }
    }
}
