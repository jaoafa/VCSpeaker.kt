package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.getProvider
import com.jaoafa.vcspeaker.tts.providers.providerOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
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
    private fun <T : ProviderContext> cacheFile(context: T) =
        VCSpeaker.cacheFolder.resolve(File("${context.hash()}.${providerOf(context).format}"))

    private fun <T : ProviderContext> create(context: T, byteArray: ByteArray): File {
        val provider = providerOf(context)
        val hash = context.hash()
        val file = cacheFile(context).apply { writeBytes(byteArray) }

        syncing {
            data += CacheData(provider.id, hash, System.currentTimeMillis())
        }

        return file
    }

    private fun <T : ProviderContext> read(context: T): File? {
        val cache = data.find { it.hash == context.hash() } ?: return null

        syncing { // update lastUsed
            data[data.indexOf(cache)] = cache.copy(lastUsed = System.currentTimeMillis())
        }

        return cacheFile(context)
    }

    suspend fun <T : ProviderContext> readOrCreate(
        context: T,
        onNoCache: suspend () -> ByteArray,
        onCached: () -> Unit
    ): File {
        val file = read(context)

        return if (file != null) {
            onCached()
            file
        } else {
            cacheFile(context).writeText("")
            create(context, onNoCache())
        }
    }


    private fun syncing(operation: () -> Unit) {
        operation()
        write()
    }

    fun initiateAuditJob(interval: Int) {
        timer("CacheAudit", false, 0, (1000 * 60 * 60 * 24 * interval).toLong()) {
            syncing {
                data.sortByDescending { it.lastUsed }
                data.drop(100).forEach {
                    val provider = getProvider(it.providerId) ?: return@forEach
                    VCSpeaker.cacheFolder.resolve(File("${it.hash}.${provider.format}")).delete()
                }
                data = data.take(100).toMutableList()
            }
        }
    }
}