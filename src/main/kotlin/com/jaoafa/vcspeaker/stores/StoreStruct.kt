package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.tools.readOrCreateAs
import com.jaoafa.vcspeaker.tools.writeAs
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

open class StoreStruct<T>(
    private val path: String,
    private val serializer: KSerializer<T>,
    deserializer: String.() -> MutableList<T> // To avoid type inference error. DO NOT REMOVE.
) {
    var data: MutableList<T> = File(path).readOrCreateAs(
        ListSerializer(serializer),
        mutableListOf(),
        deserializer
    )

    fun create(element: T): T {
        data.add(element)
        write()

        return element
    }

    fun remove(element: T): Boolean {
        val result = data.remove(element)
        write()

        return result
    }

    fun replace(from: T, to: T): T {
        remove(from)
        create(to)

        return to
    }

    fun write() {
        File(path).writeAs(ListSerializer(serializer), this.data)
    }
}