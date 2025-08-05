package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.tools.getClassesIn
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.sentry.SentryAdapter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import java.net.SocketException
import kotlin.reflect.full.createInstance

object KordStarter {
    private val logger = KotlinLogging.logger {}

    var instance: ExtensibleBot? = null
        private set

    private suspend fun prepareInstance(launch: Boolean) {
        val config = VCSpeaker.config
        val options = VCSpeaker.options

        val instance = ExtensibleBot(token = config[TokenSpec.discord]) {
            hooks {
                kordShutdownHook = false
            }

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

        this.instance = instance

        if (launch) {
            logger.info { "Starting Kord instance..." }
            instance.start()
        } else {
            logger.info { "Kord instance ready for launch." }
        }
    }

    suspend fun launch() {
        try {
            instance?.start()
        } catch (e: SocketException) {
            logger.error(e) { "Failed to connect to Discord. Retrying after 10 seconds..." }

            instance?.stop()
            delay(10000) // wait 10 seconds before retrying
            instance?.start()
        }
    }

    /**
     * Kord インスタンスを初期化します。
     *
     * @param launch 初期化が完了し次第、起動するかどうか
     */
    suspend fun start(launch: Boolean = true) = prepareInstance(launch)
}