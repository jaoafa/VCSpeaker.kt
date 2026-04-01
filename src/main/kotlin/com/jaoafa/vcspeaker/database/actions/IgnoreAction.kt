package com.jaoafa.vcspeaker.database.actions

import com.jaoafa.vcspeaker.database.tables.IgnoreEntity
import com.jaoafa.vcspeaker.database.tables.IgnoreTable
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object IgnoreAction {
    fun getIgnoresOf(guildId: Snowflake) = transaction {
        IgnoreEntity
            .find { IgnoreTable.guildDid eq guildId }
            .sortedBy { it.search.length }
            .map { it.fetchSnapshot() }
    }

    fun getEffectiveIgnoresOf(text: String, guildId: Snowflake) = getIgnoresOf(guildId).filter { it.match(text) }

    fun shouldBeIgnored(text: String, guildId: Snowflake) = getIgnoresOf(guildId).any { it.match(text) }
}
