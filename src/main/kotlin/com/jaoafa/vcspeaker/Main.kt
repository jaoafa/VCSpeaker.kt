package com.jaoafa.vcspeaker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.jaoafa.vcspeaker.commands.JoinCommand
import com.jaoafa.vcspeaker.commands.LeaveCommand
import com.jaoafa.vcspeaker.commands.SpeakCommand
import com.jaoafa.vcspeaker.config.TokenSpec
import com.jaoafa.vcspeaker.voicetext.VoiceTextAPI
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path

class Main : CliktCommand() {
    private val configPath by option(
        "-c", "--config",
        help = "The config file location."
    ).path(mustExist = true, canBeDir = false).default(Path("./config.yml"))

    private val devEnv by option(
        "-d", "--dev",
        help = "Whether to enable development environment."
    ).flag(default = false)

    private val discordToken by option(
        "-t", "--token",
        help = "The token for discord bot."
    )

    override fun run() {
        val config = Config {
            addSpec(TokenSpec)
        }.from.yaml.file(configPath.toFile())

        VCSpeaker.config = config

        VCSpeaker.voiceText = VoiceTextAPI(config[TokenSpec.voicetext])

        val token = discordToken ?: if (devEnv) config[TokenSpec.discordDev] else config[TokenSpec.discord]

        runBlocking {
            VCSpeaker.kord = ExtensibleBot(token) {
                applicationCommands {
                    enabled = true
                }

                chatCommands {
                    enabled = true
                    defaultPrefix = "#"
                }

                extensions {
                    add(::JoinCommand)
                    add(::LeaveCommand)
                    add(::SpeakCommand)
                }
            }

            VCSpeaker.kord.start()
        }
    }
}

fun main(args: Array<String>) = Main().main(args)