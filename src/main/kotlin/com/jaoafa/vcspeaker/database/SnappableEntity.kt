package com.jaoafa.vcspeaker.database

interface SnappableEntity<T : EntitySnapshot<S>, S> {
    fun fetchSnapshot(): T
}
