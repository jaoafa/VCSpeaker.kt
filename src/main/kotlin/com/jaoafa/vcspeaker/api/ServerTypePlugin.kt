package com.jaoafa.vcspeaker.api

import com.jaoafa.vcspeaker.api.types.Error
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import okhttp3.HttpUrl.Companion.toHttpUrl

class ServerTypePluginConfig {
    var type: ServerType = ServerType.Unknown
}

val invalidTypeKey = AttributeKey<Boolean>("invalidTypeKey")

val ServerTypePlugin = createApplicationPlugin(
    name = "ServerTypePlugin",
    createConfiguration = ::ServerTypePluginConfig
) {
    onCallReceive { call ->
        val path = call.request.uri.toHttpUrl().encodedPath

        if (!path.startsWith("/update")) return@onCallReceive

        val selfType = pluginConfig.type.toString().lowercase()

        if (!path.startsWith("/update/$selfType")) {
            call.respond(HttpStatusCode.BadRequest, Error("This server is ${selfType.uppercase()}."))
            call.attributes.put(invalidTypeKey, true)
        } else {
            call.attributes.put(invalidTypeKey, false)
        }
    }
}