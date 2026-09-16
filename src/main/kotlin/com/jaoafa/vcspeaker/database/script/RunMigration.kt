package com.jaoafa.vcspeaker.database.script

import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.DatabaseUtil.DEFAULT_DB_URL

fun main() {
    val databaseUrl = System.getenv("DATABASE_URL") ?: DEFAULT_DB_URL

    DatabaseUtil.migrate(databaseUrl)
}
