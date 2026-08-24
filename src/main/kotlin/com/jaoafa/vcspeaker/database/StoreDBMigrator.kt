package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.stores.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.exitProcess

@Suppress("DEPRECATION")
object StoreDBMigrator {
    private val logger = KotlinLogging.logger {}

    val stores = listOf(
        // guild dependents
        GuildStore,
        AliasStore,
        IgnoreStore,
        ReadableBotStore,
        ReadableChannelStore,
        TitleStore,
        // independents
        VisionApiCounterStore,
        VoiceStore,
        CacheStore
    )

    suspend fun run() {
        for (store in stores) {
            val name = store::class.simpleName
            logger.info { "Migrating $name to the database..." }
            try {
                store.migrateToDB()
                logger.info { "Migration complete." }
            } catch (e: StoreDBMigrationFailedException) {
                logger.error(e) { "Migration failed. Exiting..." }
                exitProcess(1)
            }
        }
    }
}
