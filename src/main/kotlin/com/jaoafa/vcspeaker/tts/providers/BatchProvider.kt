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
     * @return [AudioTrack] のリスト
     */
    suspend fun start(): List<AudioTrack> {
        val startTime = System.currentTimeMillis()

        val tracks = coroutineScope {
            val trackList = MutableList<AudioTrack?>(contexts.size) { null }

            for ((i, context) in contexts.withIndex()) {
                val provider = providerOf(context)
                    ?: throw IllegalArgumentException("Provider not found for context: ${context.describe()}")

                // Context -> Audio -> AudioTrack までロードして、即時再生できる状態にする
                launch {
                    val file = if (CacheStore.exists(context.hash())) {
                        logger.info { "Cache Found: Audio for ${context.describe()} already exists" }
                        CacheStore.read(context.hash())!!
                    } else {
                        val audio: ByteArray
                        val downloadTime = measureTimeMillis {
                            audio = provider.provide(context)
                        }

                        logger.info { "Audio Downloaded: Downloading the audio for ${context.describe()} took $downloadTime ms" }

                        CacheStore.create(context, audio)
                    }

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

            return@coroutineScope trackList
        }

        if (tracks.any { it == null })
            throw UnexpectedException("Failed to load audio tracks")

        logger.info { "Load Complete: Audio tracks loaded in ${System.currentTimeMillis() - startTime} ms" }

        return tracks.filterNotNull()
    }
}