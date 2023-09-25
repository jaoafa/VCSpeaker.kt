package com.jaoafa.vcspeaker.configs

import com.uchuhimo.konf.ConfigSpec

object EnvSpec : ConfigSpec() {

    val cacheFolder by optional<String?>(null)

    val storeFolder by optional<String?>(null)

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
    val dev by optional<Long?>(null)

    /**
     * The command prefix. The default value is `$`.
     */
    val commandPrefix by optional<String>("$")
}