package com.jaoafa.vcspeaker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.*
import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.tools.getClassesIn
import com.jaoafa.vcspeaker.tts.api.VoiceTextAPI
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.sentry.SentryAdapter
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import dev.kord.common.entity.Snowflake
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.reflect.full.createInstance

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

    private val resamplingQuality by option(
        "--resampling-quality",
        help = "The Lavaplayer resampling quality.",
        envvar = "VCSKT_RESAMPLING_QUALITY"
    ).enum<AudioConfiguration.ResamplingQuality>()

    private val encodingQuality by option(
        "--encoding-quality",
        help = "The Lavaplayer opus encoding quality.",
        envvar = "VCSKT_ENCODING_QUALITY"
    ).int().restrictTo(1..10)

    private val logger = KotlinLogging.logger {}

    override fun run() {
        logger.info { "Starting VCSpeaker..." }

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
                prefix,
                config[EnvSpec.resamplingQuality] ?: resamplingQuality ?: AudioConfiguration.ResamplingQuality.HIGH,
                config[EnvSpec.encodingQuality] ?: encodingQuality ?: 10
            )

            val instance = ExtensibleBot(discordToken) {
                applicationCommands {}

                chatCommands {
                    enabled = true
                    defaultPrefix = prefix
                }

                extensions {
                    listOf(
                        "com.jaoafa.vcspeaker.commands",
                        "com.jaoafa.vcspeaker.events"
                    ).forEach {
                        for (extensionClass in getClassesIn<Extension>(it)) {
                            add { extensionClass.kotlin.createInstance() }
                        }
                    }
                }
            }

            VCSpeaker.instance = instance

            VCSpeaker.kord = instance.kordRef

            if (finalCachePolicy != 0)
                CacheStore.initiateAuditJob(finalCachePolicy)

            if (config[TokenSpec.sentry] != null)
                instance.getKoin().get<SentryAdapter>().init {
                    dsn = config[TokenSpec.sentry]
                    environment = config[EnvSpec.sentryEnv]
                }

            instance.start()
        }
    }
}

fun main(args: Array<String>) = Main().main(args)