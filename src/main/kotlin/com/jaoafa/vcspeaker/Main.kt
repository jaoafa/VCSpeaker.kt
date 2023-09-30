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
        help = "The config file location.",
        envvar = "VCSKT_CONFIG"
    ).path(mustExist = true, canBeDir = false).default(Path("./config.yml"))

    private val storePath by option(
        "--store",
        help = "The store folder location.",
        envvar = "VCSKT_STORE"
    ).path(mustExist = false, canBeDir = true)

    private val cachePath by option(
        "--cache",
        help = "The cache folder location.",
        envvar = "VCSKT_CACHE"
    ).path(mustExist = false, canBeDir = true)

    private val cachePolicy by option(
        "--cache-policy",
        help = "The days to keep the cache."
    ).int()

    private val devId by option(
        "-d", "--dev",
        help = "The guild id for development."
    ).long()

    override fun run() {
        // Options > Config > Default

        val config = Config {
            addSpec(TokenSpec)
            addSpec(EnvSpec)
        }.from.yaml.file(configPath.toFile())

        val storeFolder = (storePath ?: Path(config[EnvSpec.storeFolder] ?: "./store")).toFile()
        val cacheFolder = (cachePath ?: Path(config[EnvSpec.cacheFolder] ?: "./cache")).toFile()

        val devId = (devId ?: config[EnvSpec.dev])?.let { Snowflake(it) }

        val finalCachePolicy = cachePolicy ?: config[EnvSpec.cachePolicy]

        val voicetextToken = config[TokenSpec.voicetext]

        val voicetext = VoiceTextAPI(voicetextToken)

        val discordToken = config[TokenSpec.discord]

        val prefix = config[EnvSpec.commandPrefix]

        runBlocking {
            VCSpeaker.init(
                voicetext,
                config,
                storeFolder,
                cacheFolder,
                devId,
                finalCachePolicy,
                prefix
            )

            VCSpeaker.instance = ExtensibleBot(discordToken) {
                applicationCommands {
                    enabled = true
                }

                chatCommands {
                    enabled = true
                    defaultPrefix = prefix
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
                    add(::SaveTitleCommand)
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

            if (finalCachePolicy != 0)
                CacheStore.initiateAuditJob(finalCachePolicy)

            VCSpeaker.instance.start()
        }
    }
}

fun main(args: Array<String>) = Main().main(args)