package com.jaoafa.vcspeaker.states

import kotlinx.serialization.Serializable

open class StateStruct<T: @Serializable Any>(initialState: T) {
    private var state = initialState
    var lock = false

    fun modify(modifier: (T) -> T) {
        if (lock) throw IllegalStateException("State is locked.")

        state = modifier(state)
    }

    fun get() = state

    fun lock() {
        lock = true
    }

    fun unlock() {
        lock = false
    }
}