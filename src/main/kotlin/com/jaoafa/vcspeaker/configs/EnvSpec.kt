package com.jaoafa.vcspeaker.configs

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

    val updateRepo by optional<String>("jaoafa/VCSpeaker.kt")

    val autoUpdate by optional<Boolean>(false)

    val lavalinkUri by optional<String>("ws://lavalink:2333")

    val lavalinkPassword by optional<String>("CHANGEME")

    val lavalinkCachePath by optional<String>("/opt/Lavalink/cache/")
}