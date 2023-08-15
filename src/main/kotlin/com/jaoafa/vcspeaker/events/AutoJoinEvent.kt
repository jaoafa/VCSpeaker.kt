package com.jaoafa.vcspeaker.events

import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.user.VoiceStateUpdateEvent

class AutoJoinEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                isNotBot()
                passIf(event.old?.getChannelOrNull() == null && event.state.getChannelOrNull() != null)
            }

            action {

            }
        }
    }
}