package com.jaoafa.vcspeaker.database.actions

import com.jaoafa.vcspeaker.database.tables.GameTable
import com.jaoafa.vcspeaker.tools.discord.GameResolver
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object GameAction {
    fun replaceAll(games: Map<Long, String>) {
        replaceAll(games, System.currentTimeMillis())
    }

    fun replaceAll(games: Map<Long, String>, timestamp: Long) {
        transaction {
            GameTable.deleteAll()
            GameTable.batchInsert(games.entries) { game ->
                this[GameTable.id] = Snowflake(game.key)
                this[GameTable.name] = game.value
            }
        }

        GameResolver.setFetchTimestamp(timestamp)
    }
}
