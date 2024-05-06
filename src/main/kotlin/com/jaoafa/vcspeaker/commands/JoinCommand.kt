package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.ChatCommandExtensions.chatCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orFallbackTo
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.move
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import io.github.oshai.kotlinlogging.KotlinLogging

@Suppress("unused")
class JoinCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    inner class JoinOptions : Options() {
        val channel by optionalChannel {
            name = "channel"
            description = "参加する VC"
            requireChannelType(ChannelType.GuildVoice)
        }
    }

    private suspend fun processJoin(
        channel: BaseVoiceChannelBehavior,
        guild: GuildBehavior,
        replier: suspend (String) -> Unit
    ) {
        if (channel.isAfk()) {
            replier("**:zzz: AFK チャンネルには接続できません。**")

            val guildName = guild.asGuildOrNull()?.name

            logger.info {
                "[$guildName] Join Failed: Join request to AFK channel rejected."
            }

            return
        }

        val selfChannel = guild.selfVoiceChannel()

        if (selfChannel != null) channel.move(replier)
        else channel.join(replier)
    }

    override suspend fun setup() {
        publicSlashCommand("join", "VC に接続します。", ::JoinOptions) {
            check { anyGuild() }
            action {
                // option > member's voice channel > error
                val channel = arguments.channel.orFallbackTo(member!!) {
                    respond(it)
                } ?: return@action

                processJoin(channel, guild!!) {
                    respond(it)
                }
            }
        }

        chatCommand("join", "VC に接続します。") {
            aliases += "summon"

            check {
                anyGuild()
                isNotBot()
            }
            action {
                val channel = member!!.getVoiceStateOrNull()?.getChannelOrNull() ?: run {
                    respond("**:question: VC に参加してください。**")
                    return@action
                }

                processJoin(channel, guild!!) {
                    respond(it)
                }
            }
        }
    }
}