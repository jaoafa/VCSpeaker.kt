package com.jaoafa.vcspeaker.store

import com.jaoafa.vcspeaker.tools.readOrCreateAs
import com.jaoafa.vcspeaker.tools.writeAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.security.MessageDigest

@Serializable
data class CacheData(
    val hash: String,
    val lastUsed: Long
)

object CacheStore : StoreStruct<CacheData> {
    override var data = File("./cache/caches.json").readOrCreateAs(
        ListSerializer(CacheData.serializer()),
        mutableListOf()
    )

    private fun hash(text: String) = MessageDigest
        .getInstance("MD5")
        .digest(text.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }

    private fun cacheFile(hash: String) = File("./cache/audio-${hash}.wav")

    fun exists(text: String) = data.find { it.hash == hash(text) } != null

    fun create(text: String, byteArray: ByteArray): File {
        val hash = hash(text)
        val file = cacheFile(hash).apply { writeBytes(byteArray) }

        data += CacheData(hash, System.currentTimeMillis())

        sync()

        return file
    }

    fun read(text: String): File? {
        val hash = hash(text)
        val cache = data.find { it.hash == hash } ?: return null

        // update lastUsed
        data[data.indexOf(cache)] = cache.copy(lastUsed = System.currentTimeMillis())

        sync()

        return cacheFile(hash)
    }

    private var act = 0

    private fun sync() {
        File("./cache/caches.json").writeAs(ListSerializer(CacheData.serializer()), data)
        if (act == 50) {
            act = 0
            audit()
        } else act++
    }

    private fun audit() {
        data.sortByDescending { it.lastUsed }
        data.drop(100).forEach { cacheFile(it.hash).delete() }
        data = data.take(100).toMutableList()
        sync()
    }

    init {
        val cacheFolder = File("./cache")

        if (!cacheFolder.exists()) cacheFolder.mkdir()
    }
}