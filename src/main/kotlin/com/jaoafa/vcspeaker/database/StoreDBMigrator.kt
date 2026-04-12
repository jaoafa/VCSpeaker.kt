package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.stores.*
import io.github.oshai.kotlinlogging.KotlinLogging

@Suppress("DEPRECATION")
object StoreDBMigrator {
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

    val logger = KotlinLogging.logger {}
    fun run() {
        for (store in stores) {
            logger.info { "Migrating ${store::class.simpleName}..." }
            store.migrateToDB()
        }
    }
}
