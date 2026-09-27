package com.jaoafa.vcspeaker.database.tables

import org.jetbrains.exposed.v1.core.Table

object KVTable : Table("kv_store") {
    val key = varchar("key", 255)
    val value = text("value")
    override val primaryKey = PrimaryKey(key)
}
