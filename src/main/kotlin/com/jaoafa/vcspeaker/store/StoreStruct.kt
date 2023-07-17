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

    fun write() {
        File(path).writeAs(ListSerializer(serializer), this.data)
    }
}