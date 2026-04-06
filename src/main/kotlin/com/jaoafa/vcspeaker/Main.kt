package com.jaoafa.vcspeaker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import com.jaoafa.vcspeaker.api.data.DataServer
import com.jaoafa.vcspeaker.api.update.UpdateServer
import com.jaoafa.vcspeaker.api.update.UpdateServerType
import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.StoreDBMigrator
import com.jaoafa.vcspeaker.tools.discord.DiscordCommandCleaner
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.system.exitProcess

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

    val updateApiPort by option(
        "--update-api-port",
        help = "The port for the Update API server to listen on.",
        envvar = "VCSKT_UPDATE_API_PORT"
    ).int()

    val dataApiPort by option(
        "--data-api-port",
        help = "The port for the Data API server to listen on.",
        envvar = "VCSKT_DATA_API_PORT"
    ).int()

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

    val clearCommandsAndExit by option(
        "--clear-commands-and-exit",
        help = "Delete all registered application commands and exit."
    ).flag()

    val migrateStoreToDB by option(
        "--migrate-store-to-db",
        help = "Migrate store data to database and exit."
    ).flag()
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

        if (options.clearCommandsAndExit) {
            val devGuildId = options.devGuildId ?: config[EnvSpec.devGuildId]

            runBlocking {
                DiscordCommandCleaner.clearRegisteredCommands(
                    token = config[TokenSpec.discord],
                    devGuildId = devGuildId
                )
            }

            logger.info { "Command cleanup completed. Exiting." }
            exitProcess(0)
        }

        if (options.migrateStoreToDB) {
            StoreDBMigrator.run(config[EnvSpec.databaseUrl])
            exitProcess(0)
        }

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

        DatabaseUtil.init(config[EnvSpec.databaseUrl])
        DatabaseUtil.createTables()

        DataServer().start(options.dataApiPort ?: config[EnvSpec.dataApiPort])

        runBlocking {
            val shouldWait = options.waitFor != null

            if (shouldWait) { // this instance is LATEST
                KordStarter.start(launch = false)
                val updateServer = UpdateServer(UpdateServerType.Latest, options.apiToken, options.waitFor)
                VCSpeaker.apiUpdateServer = updateServer
                updateServer.start(
                    options.updateApiPort ?: config[EnvSpec.updateApiPort],
                    wait = true,
                    sendBackIntSignal = true
                )
            } else {
                KordStarter.start()
            }
        }
    }
}

fun main(args: Array<String>) {
    VCSpeaker.args = args
    Entrypoint().main(args)
}
