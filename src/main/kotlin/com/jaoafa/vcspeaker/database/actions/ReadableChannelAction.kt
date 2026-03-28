package com.jaoafa.vcspeaker.database.actions

import dev.kord.core.behavior.channel.TextChannelBehavior
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import com.jaoafa.vcspeaker.database.tables.ReadableChannelEntity as Entity
import com.jaoafa.vcspeaker.database.tables.ReadableChannelTable as Table

object ReadableChannelAction {
    fun TextChannelBehavior.isReadableChannel(): Boolean = transaction {
        Entity.find {
            (Table.channelDid eq this@isReadableChannel.id) and (Table.guildDid eq guildId)
        }.count().toInt() == 1
    }
}
