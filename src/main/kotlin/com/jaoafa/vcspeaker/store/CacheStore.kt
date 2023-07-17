package com.jaoafa.vcspeaker.store

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.writeAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
data class CacheData(
    val hash: String,
    val lastUsed: Long
)

// fixme voice parameter is not considered
object CacheStore : StoreStruct<CacheData>(
    VCSpeaker.Files.caches.path,
    CacheData.serializer(),
    { Json.decodeFromString(this) }
) {
    private fun hash(text: String) = MessageDigest
        .getInstance("MD5")
        .digest(text.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }

    private fun cacheFile(hash: String) = VCSpeaker.Files.cacheFolder.resolve(File("audio-${hash}.wav"))

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
        VCSpeaker.Files.caches.writeAs(ListSerializer(CacheData.serializer()), data)
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
        val cacheFolder = VCSpeaker.Files.cacheFolder
        if (!cacheFolder.exists()) cacheFolder.mkdir()
    }
}