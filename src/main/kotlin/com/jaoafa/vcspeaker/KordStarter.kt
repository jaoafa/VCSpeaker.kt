package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.tools.getClassesIn
import com.uchuhimo.konf.Config
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.sentry.SentryAdapter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import java.net.SocketException
import kotlin.reflect.full.createInstance

object KordStarter {
    private val logger = KotlinLogging.logger {}

    private suspend fun init(options: Options, config: Config) {
        val manifest = javaClass
            .classLoader
            .getResourceAsStream("META-INF/MANIFEST.MF")
            ?.bufferedReader()
            ?.readText() ?: throw IllegalStateException("META-INF/MANIFEST.MF not found")

        val entryPrefix = "VCSpeaker-Version: "
        val version = manifest.lines().firstOrNull { it.startsWith(entryPrefix) }
            ?.removePrefix(entryPrefix) ?: "local-run-${System.currentTimeMillis()}"

        logger.info {
            "Starting VCSpeaker $version"
        }

        VCSpeaker.init(version, config, options)

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

        with(options.cachePolicy ?: config[EnvSpec.cachePolicy]) {
            if (this != 0)
                CacheStore.initiateAuditJob(this)
        }

        with(options.sentryEnv ?: config[EnvSpec.sentryEnv]) {
            if (this != null)
                instance.getKoin().get<SentryAdapter>().init {
                    it.dsn = config[TokenSpec.sentry]
                    it.environment = this
                }
        }

        instance.start()
    }

    suspend fun start(options: Options, config: Config) {
        try {
            init(options, config)
        } catch (e: SocketException) {
            logger.error(e) { "Failed to connect to Discord. Retrying after 10 seconds..." }
            // wait 10 seconds before retrying
            VCSpeaker.instance.stop()
            delay(10000)
            init(options, config)
        }
    }
}