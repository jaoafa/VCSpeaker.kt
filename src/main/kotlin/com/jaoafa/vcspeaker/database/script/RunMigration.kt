package com.jaoafa.vcspeaker.database.script

import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.DatabaseUtil.DEFAULT_DB_URL
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalDatabaseMigrationApi::class)
fun main() {
    val databaseUrl = System.getenv("DATABASE_URL") ?: DEFAULT_DB_URL

    val migration = DatabaseUtil.migrate(databaseUrl)

    val target = migration.targetSchemaVersion

    logger.info { "Migration to version $target completed successfully." }
}
