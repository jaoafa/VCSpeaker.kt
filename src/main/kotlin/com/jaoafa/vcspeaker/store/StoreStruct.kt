package com.jaoafa.vcspeaker.store

interface StoreStruct<T> {
    val data: MutableList<T>

    fun write() {

    }
}