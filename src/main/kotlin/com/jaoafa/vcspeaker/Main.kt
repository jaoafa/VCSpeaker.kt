package com.jaoafa.vcspeaker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import com.jaoafa.vcspeaker.commands.*
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.events.NewMessageEvent
import com.jaoafa.vcspeaker.events.VoiceJoinEvent
import com.jaoafa.vcspeaker.events.VoiceLeaveEvent
import com.jaoafa.vcspeaker.events.VoiceMoveEvent
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.voicetext.api.VoiceTextAPI
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path

class Main : CliktCommand() {
    private val configPath by option(
        "-c", "--config",
        help = "The config file location."
    ).path(mustExist = true, canBeDir = false).default(Path("./config.yml"))

    private val dev by option(
        "-d", "--dev",
        help = "The guild id for development."
    ).long()

    private val discordToken by option(
        "-t", "--token",
        help = "The token for discord bot."
    )

    private val cachePolicy by option(
        "--cache",
        help = "The days to keep the cache."
    ).int()

    override fun run() {
        val config = Config {
            addSpec(TokenSpec)
        }.from.yaml.file(configPath.toFile())

        VCSpeaker.config = config

        if (cachePolicy != null) {
            VCSpeaker.cachePolicy = cachePolicy!!
            CacheStore.initiateAuditJob(VCSpeaker.cachePolicy)
        }

        VCSpeaker.voiceText = VoiceTextAPI(config[TokenSpec.voicetext])

        val token = discordToken ?: if (dev != null) config[TokenSpec.discordDev] else config[TokenSpec.discord]

        VCSpeaker.dev = dev?.let { Snowflake(it) }

        runBlocking {
            VCSpeaker.instance = ExtensibleBot(token) {
                applicationCommands {
                    enabled = true
                }

                chatCommands {
                    enabled = true
                    defaultPrefix = "$"
                }

                extensions {
                    // commands
                    add(::AliasCommand)
                    add(::ClearCommand)
                    add(::IgnoreCommand)
                    add(::JoinCommand)
                    add(::LeaveCommand)
                    add(::SpeakCommand)
                    add(::TitleCommand)
                    add(::VCSpeakerCommand)
                    add(::VoiceCommand)

                    // events
                    add(::NewMessageEvent)
                    add(::VoiceJoinEvent)
                    add(::VoiceLeaveEvent)
                    add(::VoiceMoveEvent)
                }
            }

            VCSpeaker.kord = VCSpeaker.instance.kordRef
            VCSpeaker.instance.start()
        }
    }
}

fun main(args: Array<String>) = Main().main(args)