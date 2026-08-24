package com.jaoafa.vcspeaker.stores

class StoreDBMigrationFailedException(data: DBMigratableData, storeName: String, cause: Throwable? = null) : RuntimeException(
    "Migration of $data in $storeName failed.", cause
)
