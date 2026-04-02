package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.database.DatabaseUtil.DEFAULT_DB_URL
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi

val logger = KotlinLogging.logger {}

@OptIn(ExperimentalDatabaseMigrationApi::class)
fun main() {
    val databaseUrl = System.getenv("DATABASE_URL") ?: DEFAULT_DB_URL

    val flyway = Flyway.configure()
        .baselineOnMigrate(true)
        .dataSource(databaseUrl, null, null)
        .load()
    val migration = flyway.migrate()

    val target = migration.targetSchemaVersion

    logger.info { "Migration to version $target completed successfully." }
}
