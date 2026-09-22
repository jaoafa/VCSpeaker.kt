package com.jaoafa.vcspeaker.database.script

import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.DatabaseUtil.DEFAULT_DB_URL
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger { }

fun main() {
    val databaseUrl = System.getenv("DATABASE_URL") ?: DEFAULT_DB_URL

    val isMigrationSuccessful = DatabaseUtil.migrate(databaseUrl)

    if (!isMigrationSuccessful) {
        logger.error { "Database migration failed. Exiting..." }
        exitProcess(1)
    }
}
