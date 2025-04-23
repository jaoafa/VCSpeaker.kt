package com.jaoafa.vcspeaker.state

import com.jaoafa.vcspeaker.api.Server
import com.jaoafa.vcspeaker.api.ServerType

object StateManager {
    /**
     * Latest -> Locked from the start
     * Current -> Locked when the transfer happens
     */
    private var locked = Server.type() == ServerType.Latest

    fun isLocked() = locked

    fun lock() {
        locked = true
    }

    fun prepareTransfer(): State {
        locked = true
        return State.generate()
    }

    fun accept(state: State) {

    }
}