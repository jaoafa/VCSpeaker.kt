package com.jaoafa.vcspeaker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.*
import com.jaoafa.vcspeaker.api.Server
import com.jaoafa.vcspeaker.api.types.InitFinishedRequest
import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.serializer
import kotlin.io.path.Path
import kotlin.jvm.javaClass

class Options : OptionGroup("Main Options:") {
    val configPath by option(
        "-c", "--config",
        help = "The config file location.",
        envvar = "VCSKT_CONFIG"
    ).path(mustExist = true, canBeDir = false).default(Path("./config.yml"))

    val storePath by option(
        "--store",
        help = "The store folder location.",
        envvar = "VCSKT_STORE"
    ).path(mustExist = false, canBeDir = true)

    val cachePath by option(
        "--cache",
        help = "The cache folder location.",
        envvar = "VCSKT_CACHE"
    ).path(mustExist = false, canBeDir = true)

    val cachePolicy by option(
        "--cache-policy",
        help = "The days to keep the cache.",
        envvar = "VCSKT_CACHE_POLICY"
    ).int()

    val devGuildId by option(
        "-d", "--dev",
        help = "The guild id for development.",
        envvar = "VCSKT_DEV_GUILD_ID"
    ).long()

    val prefix by option(
        "-p", "--prefix",
        help = "The prefix for chat commands.",
        envvar = "VCSKT_PREFIX"
    )

    val sentryEnv by option(
        "--sentry-env",
        help = "The Sentry environment.",
        envvar = "VCSKT_SENTRY_ENV"
    )

    val resamplingQuality by option(
        "--resampling-quality",
        help = "The Lavaplayer resampling quality.",
        envvar = "VCSKT_RESAMPLING_QUALITY"
    ).enum<AudioConfiguration.ResamplingQuality>()

    val encodingQuality by option(
        "--encoding-quality",
        help = "The Lavaplayer opus encoding quality.",
        envvar = "VCSKT_ENCODING_QUALITY"
    ).int().restrictTo(1..10)

    val apiPort by option(
        "--api-port",
        help = "The port for the API server.",
        envvar = "VCSKT_API_PORT"
    ).int().default(2000)

    val waitFor by option(
        "--wait-for",
        help = "The ID of the current version of VCSpeaker.kt instance who wants to upgrade to this instance.",
        envvar = "VCSKT_WAIT_FOR"
    )

    val apiToken by option(
        "--api-token",
        help = "The token for calling the **another** VCSpeaker API server.",
        envvar = "VCSKT_API_TOKEN"
    )

    val autoUpdate by option(
        "--auto-update",
        help = "Enable auto update.",
        envvar = "VCSKT_AUTO_UPDATE"
    ).boolean()
}

class Entrypoint : CliktCommand() {
    private val logger = KotlinLogging.logger {}

    private val options by Options()

    override fun run() {
        logger.info { "Starting VCSpeaker..." }

        logger.info { "Reading config: ${options.configPath}" }

        // Options > Config > Default
        val config = Config {
            addSpec(TokenSpec)
            addSpec(EnvSpec)
        }.from.yaml.file(options.configPath.toFile())

        val manifest = javaClass
            .classLoader
            .getResourceAsStream("META-INF/MANIFEST.MF")
            ?.bufferedReader()
            ?.readText() ?: throw IllegalStateException("META-INF/MANIFEST.MF not found")

        val entryPrefix = "VCSpeaker-Version: "
        val version = manifest.lines().firstOrNull { it.startsWith(entryPrefix) }
            ?.removePrefix(entryPrefix) ?: "local-run-${System.currentTimeMillis()}"

        logger.info { "Starting VCSpeaker.kt $version" }

        VCSpeaker.init(version, config, options)

        runBlocking {
            val shouldWait = options.waitFor != null

            if (shouldWait) {
                KordStarter.start(launch = false)
                Server.start(options.apiPort)

                Server.requestUpdate(
                    "update/current/init-finished",
                    InitFinishedRequest(
                        Server.selfId,
                        Server.selfToken
                    ),
                    InitFinishedRequest.serializer()
                )

                while (true) {
                }
            } else {
                Server.start(options.apiPort)
                KordStarter.start()
            }
        }
    }
}

fun main(args: Array<String>) {
    VCSpeaker.args = args
    Entrypoint().main(args)
}