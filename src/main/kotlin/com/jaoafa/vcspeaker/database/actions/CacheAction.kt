package com.jaoafa.vcspeaker.database.actions

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.tables.SpeechCacheEntity
import com.jaoafa.vcspeaker.database.tables.SpeechCacheSnapshot
import com.jaoafa.vcspeaker.database.tables.SpeechCacheTable
import com.jaoafa.vcspeaker.database.transactionResulting
import com.jaoafa.vcspeaker.database.unwrap
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.getProvider
import com.jaoafa.vcspeaker.tts.providers.providerOf
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import kotlin.concurrent.timer
import kotlin.time.Clock

object CacheAction {
    private val logger = KotlinLogging.logger { }

    fun <T : ProviderContext> create(context: T, byteArray: ByteArray): File {
        val provider = providerOf(context)
        val file = context.getCacheFile().apply { writeBytes(byteArray) }

        transactionResulting(commit = true) {
            SpeechCacheEntity.new {
                this.providerId = provider.id
                this.hash = context.hash()
                this.lastUsedAt = Clock.System.now()
            }
        }.unwrap()

        return file
    }

    fun <T : ProviderContext> read(context: T): File? {
        val entity = transaction {
            SpeechCacheEntity.find { SpeechCacheTable.hash eq context.hash() }.singleOrNull()
        }

        if (entity == null) return null

        transactionResulting(commit = true) {
            entity.lastUsedAt = Clock.System.now()
        }.unwrap()

        return context.getCacheFile()
    }

    suspend fun <T : ProviderContext> readOrCreate(
        context: T,
        onMissProvide: suspend () -> ByteArray,
        onHit: () -> Unit
    ): File {
        val readCache = read(context)

        if (readCache != null) {
            onHit()
            return readCache
        }

        return create(context, onMissProvide())
    }

    fun cleanCache(): Int {
        val snapshots = transaction {
            SpeechCacheTable.selectAll().where {
                SpeechCacheTable.id notInSubQuery
                        SpeechCacheTable.select(SpeechCacheTable.id)
                            .orderBy(SpeechCacheTable.lastUsedAt to SortOrder.DESC).limit(100)
            }.toList().map { SpeechCacheSnapshot.from(it) }
        }

        transaction {
            SpeechCacheTable.deleteWhere { SpeechCacheTable.id inList snapshots.map { it.id } }

            for (snapshot in snapshots) {
                val provider = getProvider(snapshot.providerId) ?: continue
                VCSpeaker.cacheFolder
                    .resolve(File("${snapshot.hash}.${provider.format}"))
                    .delete()

                logger.info { "Cache ${snapshot.hash} (${snapshot.lastUsedAt}) dropped" }
            }
        }

        logger.info { "Total Cache Dropped: ${snapshots.size}" }

        return snapshots.size
    }

    fun initiateAuditJob(interval: Int) {
        timer("CacheAudit", false, 0, (1000 * 60 * 60 * 24 * interval).toLong()) {
            cleanCache()
        }
    }
}
