package com.jaoafa.vcspeaker.database.actions

import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import com.jaoafa.vcspeaker.database.tables.ReadableBotEntity as Entity
import com.jaoafa.vcspeaker.database.tables.ReadableBotTable as Table

object ReadableBotAction {
    fun UserBehavior.isReadableBotOn(guild: GuildBehavior) = transaction {
        Entity.find {
            (Table.botDid eq this@isReadableBotOn.id) and (Table.guildDid eq guild.id)
        }.count().toInt() == 1
    }
}