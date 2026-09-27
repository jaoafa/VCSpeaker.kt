package com.jaoafa.vcspeaker.stores

import kotlinx.serialization.Serializable

@Serializable
abstract class DBMigratableData {
    var migrated: Boolean = false
        private set

    abstract fun migrationTransaction()

    fun migrateEntryToDB() {
        migrationTransaction()
        migrated = true
    }
}
