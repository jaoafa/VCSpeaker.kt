package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.database.tables.*
import dev.kord.core.behavior.GuildBehavior
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.SizedIterable
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

    /**
     * GUILD テーブルに登録されている Guild のレコードを取得します。登録されていない場合は null を返します。
     */
    fun GuildBehavior.getEntityOrNull() = transaction {
        GuildEntity.findById(this@getEntityOrNull.id)
    }

    /**
     * GUILD テーブルに登録されている Guild のレコードを取得します。登録されていない場合は [IllegalStateException] をスローします。
     * check { anyGuildRegistered() } でチェックされた後に使用されることを想定しています。
     */
    fun GuildBehavior.getEntity() =
        getEntityOrNull() ?: throw IllegalStateException("Guild ${id.value} is not registered.")

    fun Table.version() = integer("version").default(0)

    inline fun <reified T : TypedRow, reified E : TypedEntity<T>> SizedIterable<E>.getRows() = map { it.getRow() }
}
