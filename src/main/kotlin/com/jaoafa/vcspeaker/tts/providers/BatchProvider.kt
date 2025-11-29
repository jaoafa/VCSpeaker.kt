package com.jaoafa.vcspeaker.tts.providers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.stores.CacheStore
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.rest.loadItem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.rmi.UnexpectedException
import kotlin.system.measureTimeMillis

class BatchProvider(private val link: Link, private val contexts: List<ProviderContext>) {
    private val logger = KotlinLogging.logger { }

    /**
     * 与えられた [ProviderContext] のリストから [Track] のリストを生成します。
     *
     * @throws IllegalArgumentException Context に対応する Provider が存在しない場合
     * @throws HttpRequestTimeoutException リクエストがタイムアウトした場合
     * @throws IOException リクエストが失敗した場合
     *
     * @return [Track] のリスト
     */
    suspend fun start(): List<Track> {
        val startTime = System.currentTimeMillis()

        val (tracks, links) = coroutineScope {
            val trackList = MutableList<Track?>(contexts.size) { null }

            val duplicateLinks = mutableMapOf<Int, Int>()

            for ((i, context) in contexts.withIndex()) {
                if (contexts.take(i).contains(context)) {
                    val refIndex = contexts.indexOf(context)
                    duplicateLinks[i] = refIndex

                    logger.info {
                        "Duplicate Found: Duplicate of ${context.describe()} found at index $refIndex"
                    }

                    continue
                }

                val provider = providerOf(context)

                // Context -> Audio -> AudioTrack までロードして、即時再生できる状態にする
                launch {
                    val file = CacheStore.readOrCreate(context, onNoCache = {
                        val audio: ByteArray
                        val downloadTime = measureTimeMillis {
                            audio = provider.provide(context)
                        }
                        logger.info { "Audio Downloaded: Downloading the audio for ${context.describe()} took $downloadTime ms" }

                        audio
                    }, onCached = {
                        logger.info { "Cache Found: Audio for ${context.describe()} already exists" }
                    })

                    val track = when (val item = link.loadItem(VCSpeaker.config[EnvSpec.lavalinkCachePath] + file.name)) {
                        is LoadResult.TrackLoaded -> item.data
                        is LoadResult.LoadFailed -> throw UnexpectedException(item.data.causeStackTrace)
                        else -> throw UnexpectedException("Code should not reach here")
                    }

                    trackList[i] = track
                }
            }

            return@coroutineScope trackList to duplicateLinks
        }

        for ((i, refIndex) in links) {
            tracks[i] = tracks[refIndex]?.copy()
                ?: throw UnexpectedException("Failed to clone audio track")
        }

        if (tracks.any { it == null })
            throw UnexpectedException("Failed to load audio tracks")

        logger.info { "Load Complete: Audio tracks loaded in ${System.currentTimeMillis() - startTime} ms" }

        return tracks.filterNotNull()
    }
}