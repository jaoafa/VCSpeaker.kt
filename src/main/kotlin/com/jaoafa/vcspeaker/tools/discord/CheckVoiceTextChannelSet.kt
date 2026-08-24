package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.database.actions.GuildAction.getVoiceTextChannelOrNull
import dev.kord.core.event.Event
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.types.CheckContext

suspend fun <T : Event> CheckContext<T>.isVoiceTextChannelSet() {
    if (!passed) return

    val guild = guildFor(event) ?: run {
        fail()
        return
    }

    if (guild.getVoiceTextChannelOrNull() == null) {
        fail()
        return
    }

    pass()
}