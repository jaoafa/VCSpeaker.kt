package com.jaoafa.vcspeaker.reload.state

import com.jaoafa.vcspeaker.api.Server
import com.jaoafa.vcspeaker.api.ServerType

object StateManager {
    /**
     * Latest -> Locked from the start
     * Current -> Locked when the transfer happens
     */
    var locked = Server.type == ServerType.Latest
        private set

    fun lock() {
        locked = true
    }

    fun prepareTransfer(): State {
        locked = true
        return State.generate()
    }

    fun accept(state: State) {
        TODO()
    }
}