package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.database.tables.*
import dev.kord.core.behavior.GuildBehavior
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseUtil {
    val tables = listOf(
        AliasTable,
        GuildTable,
        IgnoreTable,
        ReadableBotTable,
        ReadableChannelTable,
        SpeechCacheTable,
        UserTable,
        VCTitleTable,
        VisionAPICounterTable,
        VoiceTable
    )

    fun init(): Database {
        val db = Database.connect(
            "jdbc:h2:file:./database/h2;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE",
            driver = "org.h2.Driver"
        ) // fixme: env var, place under ./database/ or something
        TransactionManager.defaultDatabase = db

        return db
    }

    fun createTables() {
        transaction {
            for (table in tables) {
                SchemaUtils.create(table)
            }
        }
    }

    fun GuildBehavior.getEntityOrNull(): GuildEntity? {
        return transaction {
            GuildEntity.findById(this@getEntityOrNull.id)
        }
    }

    fun GuildBehavior.isNotRegistered() = getEntityOrNull() == null

    fun Table.version() = integer("version").default(0)
}