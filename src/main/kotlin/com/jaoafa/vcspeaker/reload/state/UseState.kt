package com.jaoafa.vcspeaker.reload.state

abstract class UseState<T : StateEntry> {
    var locked = false
        private set
        get() = locked

    fun lock() {
        locked = true
    }

    abstract fun prepareTransfer(): T
}