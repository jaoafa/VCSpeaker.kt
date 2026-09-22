package com.jaoafa.vcspeaker.database

interface OwnedEntity<T> {
    val ownerId: T

    fun isOwnedBy(ownerId: T) = this.ownerId == ownerId
}

fun <E : OwnedEntity<T>, T> E.takeIfOwnedBy(owner: T): E? =
    takeIf { it.isOwnedBy(owner) }
