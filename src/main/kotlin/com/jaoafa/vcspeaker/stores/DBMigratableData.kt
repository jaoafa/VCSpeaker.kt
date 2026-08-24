package com.jaoafa.vcspeaker.stores

abstract class DBMigratableData {
    var migrated: Boolean = false
        private set

    abstract fun migrationTransaction()

    fun migrateEntryToDB() {
        migrationTransaction()
        migrated = true
    }
}
