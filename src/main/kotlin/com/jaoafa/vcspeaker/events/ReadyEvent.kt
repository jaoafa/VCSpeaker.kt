package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.Server
import com.jaoafa.vcspeaker.api.ServerType
import com.jaoafa.vcspeaker.reload.Reload
import com.jaoafa.vcspeaker.reload.state.StateManager
import dev.kord.core.event.gateway.ReadyEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import io.github.oshai.kotlinlogging.KotlinLogging

class ReadyEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    override suspend fun setup() {
        event<ReadyEvent> {
            action {
                logger.info { "Ready! Logged in as ${event.self.tag}." }

                if (VCSpeaker.apiServer?.type == ServerType.Latest) {
                    StateManager.reconnect()
                }

                event.kord.editPresence {
                    watching("users talk | VCSpeaker.kt ${VCSpeaker.version}")
                }

                Reload.initAutoUpdate()
            }
        }
    }
}