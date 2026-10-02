package com.jaoafa.vcspeaker.tools.kvstore

import kotlinx.serialization.KSerializer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class KVProperty<T>(
    private val serializer: KSerializer<T>,
    private val key: String? = null
) : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return KVUtil.get(key ?: property.name, serializer)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (value != null) {
            KVUtil.set(key ?: property.name, value, serializer)
        } else {
            KVUtil.delete(key ?: property.name)
        }
    }
}

class KVPropertyWithDefault<T>(
    serializer: KSerializer<T>,
    key: String? = null,
    private val defaultValue: T
) : KVProperty<T>(serializer, key) {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return super.getValue(thisRef, property) ?: defaultValue
    }
}
