package com.jaoafa.vcspeaker.configs

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.uchuhimo.konf.ConfigSpec

object EnvSpec : ConfigSpec() {
    val storeFolder by optional<String>("./store")

    val cacheFolder by optional<String>("./cache")

    /**
     * The days to keep the cache.
     * The default value is 10(days).
     * 0 to disable the cache deletion.
     */
    val cachePolicy by optional<Int>(10)

    /**
     * The guild id for development.
     * VCSpeaker will launch in development mode when this is set.
     */
    val devGuildId by optional<Long?>(null)

    /**
     * The command prefix. The default value is `$`.
     */
    val commandPrefix by optional<String>("$")

    /**
     * The Sentry environment.
     */
    val sentryEnv by optional<String?>(null)

    val resamplingQuality by optional<AudioConfiguration.ResamplingQuality>(AudioConfiguration.ResamplingQuality.HIGH)

    val encodingQuality by optional<Int>(10)
}