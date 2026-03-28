package com.jaoafa.vcspeaker.database

import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import java.io.File

const val MIGRATION_PATH = "src/main/resources/db/migration"

@OptIn(ExperimentalDatabaseMigrationApi::class)
fun main() {
    DatabaseUtil.init()
    val migrationDir = File(MIGRATION_PATH)
    migrationDir.mkdirs()
    transaction {
        MigrationUtils.generateMigrationScript(
            *DatabaseUtil.tables.toTypedArray(),
            scriptDirectory = migrationDir.path,
            scriptName = System.getenv("MIGRATION_NAME") ?: "migration"
        )
    }
}