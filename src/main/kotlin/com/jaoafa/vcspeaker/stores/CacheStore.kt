package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.getProvider
import com.jaoafa.vcspeaker.tts.providers.providerOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CacheData(
    val providerId: String,
    val hash: String,
    val lastUsed: Long
)

object CacheStore : StoreStruct<CacheData>(
    VCSpeaker.Files.caches.path,
    CacheData.serializer(),
    { Json.decodeFromString(this) },

    version = 1,
    migrators = mapOf(
        1 to { file ->
            file.delete()

            VCSpeaker.cacheFolder.listFiles()?.forEach {
                it.delete()
            }

            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(CacheData.serializer()),
                    TypedStore(1, mutableListOf())
                )
            )
        }
    ),
    auditor = { data ->
        data.filter {
            val provider = getProvider(it.providerId)

            if (provider == null) {
                VCSpeaker.cacheFolder.listFiles()?.forEach { file ->
                    if (file.nameWithoutExtension == it.hash) file.delete()
                }
                return@filter false
            }

            val fileExists = VCSpeaker.cacheFolder.resolve(File("${it.hash}.${provider.format}")).exists()

            return@filter fileExists
        }.toMutableList()
    }
) {
    private val pendingMutex = Mutex()
    private val pendingByHash = mutableMapOf<String, Deferred<File>>()
    private val auditScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun <T : ProviderContext> cacheFile(context: T) =
        VCSpeaker.cacheFolder.resolve(File("${context.hash()}.${providerOf(context).format}"))

    private suspend fun <T : ProviderContext> create(context: T, byteArray: ByteArray): File {
        val provider = providerOf(context)
        val hash = context.hash()
        val file = cacheFile(context).apply { writeBytes(byteArray) }

        withData {
            data += CacheData(provider.id, hash, System.currentTimeMillis())
            writeLocked()
        }

        return file
    }

    private suspend fun <T : ProviderContext> read(context: T): File? = withData {
        val index = data.indexOfFirst { it.hash == context.hash() }
        if (index == -1) return@withData null

        data[index] = data[index].copy(lastUsed = System.currentTimeMillis())
        writeLocked()

        cacheFile(context)
    }

    suspend fun <T : ProviderContext> readOrCreate(
        context: T,
        onNoCache: suspend () -> ByteArray,
        onCached: () -> Unit
    ): File {
        val hash = context.hash()

        val deferred = pendingMutex.withLock {
            pendingByHash.getOrPut(hash) {
                fetchScope.async {
                    val file = read(context)
                    if (file != null) {
                        onCached()
                        file
                    } else {
                        cacheFile(context).writeText("")
                        create(context, onNoCache())
                    }
                }
            }
        }

        return try {
            deferred.await()
        } finally {
            pendingMutex.withLock { pendingByHash.remove(hash) }
        }
    }

    fun initiateAuditJob(interval: Int) {
        auditScope.launch {
            while (isActive) {
                withData {
                    data.sortByDescending { it.lastUsed }
                    data.drop(100).forEach {
                        val provider = getProvider(it.providerId) ?: return@forEach
                        VCSpeaker.cacheFolder.resolve(File("${it.hash}.${provider.format}")).delete()
                    }
                    data = data.take(100).toMutableList()
                    writeLocked()
                }
                delay(1000L * 60 * 60 * 24 * interval)
            }
        }
    }
}
