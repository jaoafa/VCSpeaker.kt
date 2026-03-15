package com.jaoafa.vcspeaker.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi

val logger = KotlinLogging.logger {}

@OptIn(ExperimentalDatabaseMigrationApi::class)
fun main() {
    val flyway = Flyway.configure()
        .baselineOnMigrate(true)
        .dataSource("jdbc:h2:file:./database/h2;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE", null, null)
        .load()
    val migration = flyway.migrate()

    val target = migration.targetSchemaVersion

    logger.info { "Migration to version $target completed successfully." }
}