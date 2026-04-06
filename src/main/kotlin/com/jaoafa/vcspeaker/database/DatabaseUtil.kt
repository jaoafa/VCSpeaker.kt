package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.database.tables.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseUtil {
    private val logger = KotlinLogging.logger { }

    const val DEFAULT_DB_URL = "jdbc:h2:file:./database/h2;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"

    val tables = listOf(
        VoiceTable,
        GuildTable,
        AliasTable,
        IgnoreTable,
        ReadableBotTable,
        ReadableChannelTable,
        SpeechCacheTable,
        UserTable,
        VCTitleTable,
        VisionAPICounterTable
    )

    fun connect(url: String): Database {
        val db = Database.connect(url, driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db

        return db
    }

    fun migrate(url: String): MigrateResult {
        val flyway = Flyway.configure()
            .baselineOnMigrate(true)
            .dataSource(url, null, null)
            .load()
        return flyway.migrate()
    }

    fun createTables() {
        transaction {
            val tablesBefore = SchemaUtils.listTables()
            logger.info { "Creating tables... (Current: $tablesBefore)" }
            SchemaUtils.create(*tables.toTypedArray(), inBatch = true)

            val tablesAfter = SchemaUtils.listTables().mapNotNull { it.takeIf { !tablesBefore.contains(it) } }
            logger.info { "Tables created: $tablesAfter" }
        }
    }

    fun Table.version() = integer("version").default(0)

    inline fun <reified E : SnappableEntity<T, S>, T : EntitySnapshot<S>, S> SizedIterable<E>.getSnapshots() =
        map { it.getSnapshot() }
}
