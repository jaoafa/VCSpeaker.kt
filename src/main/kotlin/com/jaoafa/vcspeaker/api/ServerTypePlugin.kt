package com.jaoafa.vcspeaker.api

import com.jaoafa.vcspeaker.api.types.Error
import com.uchuhimo.konf.toPath
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import okhttp3.HttpUrl.Companion.toHttpUrl

class ServerTypePluginConfig {
    var type: ServerType = ServerType.Unknown
}

// If the wrong type of endpoint is called, this attribute will be set to true.
val invalidTypeKey = AttributeKey<Boolean>("invalidTypeKey")

val ServerTypePlugin = createApplicationPlugin(
    name = "ServerTypePlugin",
    createConfiguration = ::ServerTypePluginConfig
) {
    onCall { call ->
        val path = call.request.uri

        println("Processing a request to $path")

        if (!path.startsWith("/update")) return@onCall

        val selfType = pluginConfig.type.toString().lowercase()

        if (!path.startsWith("/update/$selfType")) {
            call.respond(HttpStatusCode.BadRequest, Error("This server is ${selfType.uppercase()}."))
            call.attributes.put(invalidTypeKey, true)
        } else {
            call.attributes.put(invalidTypeKey, false)
        }
    }
}