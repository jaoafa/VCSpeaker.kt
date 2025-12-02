package com.jaoafa.vcspeaker

import dev.kord.core.Kord
import dev.schlaubi.lavakord.kord.lavakord

fun initLavaLink(kord: Kord, uri: String, password: String) {
    try {
        val lavalink = kord.lavakord()
        lavalink.addNode(uri, password)
        VCSpeaker.lavalink = lavalink
    } catch (e: Exception) {
        println("Failed to initialize LavaLink connection to $uri: ${e.message}")
        throw IllegalStateException("Failed to initialize LavaLink connection to $uri", e)
    }
}