package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.database.tables.*
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseUtil {
    const val DEFAULT_DB_URL = "jdbc:h2:file:./database/h2;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"

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

    fun init(url: String = DEFAULT_DB_URL): Database {
        val db = Database.connect(url, driver = "org.h2.Driver") // fixme: env var, place under ./database/ or something
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

    fun Table.version() = integer("version").default(0)

    inline fun <reified E : SnappableEntity<T, S>, T : EntitySnapshot<S>, S> SizedIterable<E>.fetchSnapshots() =
        map { it.fetchSnapshot() }
}
