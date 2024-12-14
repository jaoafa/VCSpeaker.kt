package com.jaoafa.vcspeaker.states

object State {
    val connection = ConnectionState()
    val queue = QueueState()

    fun lockAll() {
        connection.lock()
        queue.lock()
    }
}