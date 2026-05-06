package com.jaoafa.vcspeaker.stores

interface DBMigratableData {
    var migrated: Boolean
    fun migrate()
}
