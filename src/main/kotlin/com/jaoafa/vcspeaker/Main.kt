package com.jaoafa.vcspeaker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import com.jaoafa.vcspeaker.commands.*
import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.events.*
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

    private val cachePolicy by option(
        "--cache",
        help = "The days to keep the cache."
    ).int()

    private val dev by option(
        "-d", "--dev",
        help = "The guild id for development."
    ).long()

    override fun run() {
        val config = Config {
            addSpec(TokenSpec)
            addSpec(EnvSpec)
        }.from.yaml.file(configPath.toFile())

        VCSpeaker.config = config

        VCSpeaker.dev = (dev ?: config[EnvSpec.dev])?.let { Snowflake(it) }

        val finalCachePolicy = cachePolicy ?: config[EnvSpec.cachePolicy]
        if (finalCachePolicy != 0) {
            VCSpeaker.cachePolicy = finalCachePolicy
            CacheStore.initiateAuditJob(finalCachePolicy)
        }

        val voicetextToken = if (VCSpeaker.isDev()) {
            config[TokenSpec.voicetextDev] ?: throw IllegalStateException("VoiceText API token for dev is not set.")
        } else {
            config[TokenSpec.voicetext]
        }

        VCSpeaker.voiceText = VoiceTextAPI(voicetextToken)

        val discordToken = if (VCSpeaker.isDev()) {
            config[TokenSpec.discordDev] ?: throw IllegalStateException("Discord API token for dev is not set.")
        } else {
            config[TokenSpec.discord]
        }

        runBlocking {
            VCSpeaker.instance = ExtensibleBot(discordToken) {
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
                    add(::ResetTitleCommand)
                    add(::VCSpeakerCommand)
                    add(::VoiceCommand)

                    // events
                    add(::NewMessageEvent)
                    add(::VoiceJoinEvent)
                    add(::VoiceLeaveEvent)
                    add(::VoiceMoveEvent)
                    add(::TitleEvent)
                }
            }

            VCSpeaker.kord = VCSpeaker.instance.kordRef
            VCSpeaker.instance.start()
        }
    }
}

fun main(args: Array<String>) = Main().main(args)