package com.jaoafa.vcspeaker.database

import org.jetbrains.exposed.v1.core.ResultRow

abstract class EntitySnapshot<E> {
    abstract fun getEntity(): E
    open fun describe(): String = toString()
}

interface SnapshotFactory<S> {
    fun from(row: ResultRow): S
}
