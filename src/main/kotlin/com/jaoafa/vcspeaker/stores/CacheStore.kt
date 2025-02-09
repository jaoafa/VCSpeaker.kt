package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.hashMd5
import com.jaoafa.vcspeaker.tools.writeAs
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.SpeechProvider
import com.jaoafa.vcspeaker.tts.providers.getProvider
import com.jaoafa.vcspeaker.tts.providers.providerOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import kotlin.concurrent.timer

@Serializable
data class CacheData(
    val providerId: String,
    val hash: String,
    val lastUsed: Long
)

object CacheStore : StoreStruct<CacheData>(
    VCSpeaker.Files.caches.path,
    CacheData.serializer(),
    { Json.decodeFromString(this) }
) {
    private fun cacheFile(hash: String, ext: String) = VCSpeaker.cacheFolder.resolve(File("${hash}.$ext"))

    fun exists(hash: String) = data.find { it.hash == hash } != null

    fun <T : ProviderContext> create(context: T, byteArray: ByteArray): File {
        val provider = providerOf(context) ?: throw IllegalArgumentException("Provider not found")
        val hash = context.hash()
        val file = cacheFile(hash, provider.format).apply { writeBytes(byteArray) }

        syncing {
            data += CacheData(provider.id, hash, System.currentTimeMillis())
        }

        return file
    }

    fun read(hash: String): File? {
        val cache = data.find { it.hash == hash } ?: return null

        syncing { // update lastUsed
            data[data.indexOf(cache)] = cache.copy(lastUsed = System.currentTimeMillis())
        }

        val format = getProvider(cache.providerId)?.format ?: return null

        return cacheFile(hash, format)
    }


    private fun syncing(operation: () -> Unit) {
        operation()
        VCSpeaker.Files.caches.writeAs(ListSerializer(CacheData.serializer()), data)
    }

    fun initiateAuditJob(interval: Int) {
        timer("CacheAudit", false, 0, (1000 * 60 * 60 * 24 * interval).toLong()) {
            syncing {
                data.sortByDescending { it.lastUsed }
                data.drop(100).forEach {
                    val provider = getProvider(it.providerId) ?: return@forEach
                    cacheFile(it.hash, provider.format).delete()
                }
                data = data.take(100).toMutableList()
            }
        }
    }

    init {
        val cacheFolder = VCSpeaker.cacheFolder
        if (!cacheFolder.exists()) cacheFolder.mkdir()
    }
}