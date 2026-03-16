package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.database.tables.AliasTable
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.GuildTable
import com.jaoafa.vcspeaker.database.tables.IgnoreTable
import com.jaoafa.vcspeaker.database.tables.ReadableBotTable
import com.jaoafa.vcspeaker.database.tables.ReadableChannelTable
import com.jaoafa.vcspeaker.database.tables.SpeechCacheTable
import com.jaoafa.vcspeaker.database.tables.UserTable
import com.jaoafa.vcspeaker.database.tables.VCTitleTable
import com.jaoafa.vcspeaker.database.tables.VisionAPICounterTable
import com.jaoafa.vcspeaker.database.tables.VoiceTable
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.toLong
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
            GuildEntity.findById(this@getEntityOrNull.id.toLong())
        }
    }

    fun Table.version() = integer("version").default(0)
}