package com.jaoafa.vcspeaker.events

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.gateway.ReadyEvent
import io.github.oshai.kotlinlogging.KotlinLogging

class ReadyEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    override suspend fun setup() {
        event<ReadyEvent> {
            action {
                logger.info { "Ready! Logged in as ${event.self.tag}." }
            }
        }
    }
}