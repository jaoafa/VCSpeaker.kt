package com.jaoafa.vcspeaker.tools

import io.ktor.client.*
import io.ktor.client.plugins.*

@Suppress("FunctionName")
fun HttpClientConfig<*>.VCSpeakerUserAgent() {
    install(UserAgent) {
        agent = "jaoafa/VCSpeaker.kt (https://github.com/jaoafa/VCSpeaker.kt)"
    }
}