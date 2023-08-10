package com.jaoafa.vcspeaker.store

import com.jaoafa.vcspeaker.tools.readOrCreateAs
import com.jaoafa.vcspeaker.tools.writeAs
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

open class StoreStruct<T>(
    private val path: String,
    private val serializer: KSerializer<T>,
    deserializer: String.() -> MutableList<T> // To avoid type inference error
) {
    var data: MutableList<T> = File(path).readOrCreateAs(
        ListSerializer(serializer),
        mutableListOf(),
        deserializer
    )

    fun create(element: T) {
        data.add(element)
        write()
    }

    fun remove(element: T): Boolean {
        val result = data.remove(element)
        write()

        return result
    }

    fun removeIf(predicate: (T) -> Boolean) {
        data.removeIf(predicate)
        write()
    }

    fun write() {
        File(path).writeAs(ListSerializer(serializer), this.data)
    }
}