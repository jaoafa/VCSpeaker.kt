package com.jaoafa.vcspeaker

import dev.kord.core.Kord
import dev.schlaubi.lavakord.kord.lavakord

fun initLavaLink(kord: Kord, uri: String, password: String) {
    val lavalink = kord.lavakord()

    lavalink.addNode(uri, password)

    VCSpeaker.lavalink = lavalink
}