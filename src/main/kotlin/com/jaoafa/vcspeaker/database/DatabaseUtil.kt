package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.database.tables.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseUtil {
    private val logger = KotlinLogging.logger { }

    const val DEFAULT_DB_URL = "jdbc:h2:file:./database/h2;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"

    val tables: List<Table> = listOf(
        VoiceTable,
        GuildTable,
        AliasTable,
        IgnoreTable,
        ReadableBotTable,
        ReadableChannelTable,
        SpeechCacheTable,
        UserTable,
        VCTitleTable,
        VisionAPICounterTable,
        GameTable,
        KVTable
    )

    fun connect(url: String): Database {
        val db = Database.connect(url, driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        return db
    }

    fun migrate(url: String): Boolean {
        val flywayLoader = Flyway.configure()
            .dataSource(url, null, null)
            .load()

        val result = try {
            val info = flywayLoader.info().all()
            val latest = info.maxOf { it.version }

            logger.info { "Starting migration to $latest..." }

            val flyway = Flyway.configure()
                .baselineOnMigrate(true)
                .baselineVersion(latest)
                .baselineDescription("Initialization Baseline")
                .dataSource(url, null, null)
                .load()

            flyway.migrate()
        } catch (e: FlywayException) {
            logger.error(e) { "Migration failed." }
            return false
        }

        logger.info { "Migration from ${result.initialSchemaVersion} to ${result.targetSchemaVersion} was successful." }
        return true
    }

    fun createTables(vararg tables: Table) {
        val tablesAfter = transaction {
            val tablesBefore = SchemaUtils.listTables()
            logger.info { "Creating tables... (Current: $tablesBefore)" }
            SchemaUtils.create(*tables, inBatch = true)

            SchemaUtils.listTables().mapNotNull { it.takeIf { !tablesBefore.contains(it) } }
        }
        logger.info { "Tables created: $tablesAfter" }
    }

    fun createAllTables() = createTables(*tables.toTypedArray())

    fun dropTables(vararg tables: Table) {
        transaction {
            logger.info { "Dropping all tables..." }
            SchemaUtils.drop(*tables, inBatch = true)
        }
        logger.info { "Dropped all tables." }
    }

    fun dropAllTables() = dropTables(*tables.toTypedArray())

    fun Table.version() = integer("version").default(0)

    inline fun <reified E : SnappableEntity<T, S>, T : EntitySnapshot<S>, S> SizedIterable<E>.getSnapshots() =
        map { it.getSnapshot() }
}
