package com.jaoafa.vcspeaker.database

interface TypedEntity<T : TypedRow> {
    fun getRow(): T
}