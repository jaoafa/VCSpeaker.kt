package com.jaoafa.vcspeaker.tools.discord

import dev.kord.core.event.Event
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.checks.userFor

suspend fun <T : Event> CheckContext<T>.isNotSelf() {
    if (!passed) return

    val user = userFor(event) ?: run {
        fail()
        return
    }

    if (user.id == event.kord.selfId) {
        fail()
        return
    }

    pass()
}