package com.jaoafa.vcspeaker.tts.providers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.CacheStore
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.rmi.UnexpectedException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.system.measureTimeMillis

class BatchProvider(private val contexts: List<ProviderContext>) {
    private val logger = KotlinLogging.logger { }

    /**
     * 与えられた [ProviderContext] のリストから [AudioTrack] のリストを生成します。
     *
     * @throws IllegalArgumentException Context に対応する Provider が存在しない場合
     * @throws HttpRequestTimeoutException リクエストがタイムアウトした場合
     * @throws IOException リクエストが失敗した場合
     *
     * @return [AudioTrack] のリスト
     */
    suspend fun start(): List<AudioTrack> {
        val startTime = System.currentTimeMillis()

        val (tracks, links) = coroutineScope {
            val trackList = MutableList<AudioTrack?>(contexts.size) { null }

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

                    val track = suspendCoroutine { continuation ->
                        VCSpeaker.lavaplayer.loadItemOrdered(
                            VCSpeaker.lavaplayer,
                            file.path,
                            object : AudioLoadResultHandler {
                                override fun trackLoaded(track: AudioTrack) {
                                    continuation.resume(track)
                                }

                                override fun playlistLoaded(playlist: AudioPlaylist) =
                                    continuation.resumeWithException(UnexpectedException("This code should not be reached."))

                                override fun noMatches() =
                                    continuation.resumeWithException(UnexpectedException("This code should not be reached."))

                                override fun loadFailed(exception: FriendlyException) =
                                    continuation.resumeWithException(exception)

                            }
                        )
                    }

                    trackList[i] = track
                }
            }

            return@coroutineScope trackList to duplicateLinks
        }

        for ((i, refIndex) in links) {
            tracks[i] = tracks[refIndex]?.makeClone()
                ?: throw UnexpectedException("Failed to clone audio track")
        }

        if (tracks.any { it == null })
            throw UnexpectedException("Failed to load audio tracks")

        logger.info { "Load Complete: Audio tracks loaded in ${System.currentTimeMillis() - startTime} ms" }

        return tracks.filterNotNull()
    }
}