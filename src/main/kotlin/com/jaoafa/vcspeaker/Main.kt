package com.jaoafa.vcspeaker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.*
import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.tools.getClassesIn
import com.jaoafa.vcspeaker.tts.api.VoiceTextAPI
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import dev.kord.common.entity.Snowflake
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.sentry.SentryAdapter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.SocketException
import kotlin.io.path.Path
import kotlin.reflect.full.createInstance

class Main : CliktCommand() {
    private val logger = KotlinLogging.logger {}

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
        help = "The days to keep the cache.",
        envvar = "VCSKT_CACHE_POLICY"
    ).int()

    private val devGuildId by option(
        "-d", "--dev",
        help = "The guild id for development.",
        envvar = "VCSKT_DEV_GUILD_ID"
    ).long()

    private val prefix by option(
        "-p", "--prefix",
        help = "The prefix for chat commands.",
        envvar = "VCSKT_PREFIX"
    )

    private val sentryEnv by option(
        "--sentry-env",
        help = "The Sentry environment.",
        envvar = "VCSKT_SENTRY_ENV"
    )

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

    override fun run() {
        logger.info { "Starting VCSpeaker..." }

        logger.info { "Reading config: $configPath" }

        // Options > Config > Default
        val config = Config {
            addSpec(TokenSpec)
            addSpec(EnvSpec)
        }.from.yaml.file(configPath.toFile())

        suspend fun init() {
            VCSpeaker.init(
                config = config,
                voicetext = VoiceTextAPI(apiKey = config[TokenSpec.voicetext]),
                storeFolder = (storePath ?: Path(config[EnvSpec.storeFolder])).toFile(),
                cacheFolder = (cachePath ?: Path(config[EnvSpec.cacheFolder])).toFile(),
                devGuildId = (devGuildId ?: config[EnvSpec.devGuildId])?.let { Snowflake(it) },
                prefix = prefix ?: config[EnvSpec.commandPrefix],
                resamplingQuality = resamplingQuality ?: config[EnvSpec.resamplingQuality],
                encodingQuality = encodingQuality ?: config[EnvSpec.encodingQuality]
            )

            val instance = ExtensibleBot(token = config[TokenSpec.discord]) {
                applicationCommands {}

                chatCommands {
                    enabled = true
                    defaultPrefix = VCSpeaker.prefix
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

            with(cachePolicy ?: config[EnvSpec.cachePolicy]) {
                if (this != 0)
                    CacheStore.initiateAuditJob(this)
            }

            with(sentryEnv ?: config[EnvSpec.sentryEnv]) {
                if (this != null)
                    instance.getKoin().get<SentryAdapter>().init {
                        it.dsn = config[TokenSpec.sentry]
                        it.environment = this
                    }
            }

            instance.start()
        }

        suspend fun retryLoop() {
            try {
                init()
            } catch (e: SocketException) {
                logger.error(e) { "Failed to connect to Discord. Retrying after 10 seconds..." }
                // wait 10 seconds before retrying
                VCSpeaker.instance.stop()
                delay(10000)
                retryLoop()
            }
        }

        runBlocking { retryLoop() }
    }
}

fun main(args: Array<String>) = Main().main(args)