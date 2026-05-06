package com.jaoafa.vcspeaker.api.update

import com.jaoafa.vcspeaker.api.update.types.UpdateError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*

class UpdateServerTypePluginConfig {
    var type: UpdateServerType = UpdateServerType.Unknown
}

// If the wrong type of endpoint is called, this attribute will be set to true.
val invalidTypeKey = AttributeKey<Boolean>("invalidTypeKey")

val UpdateServerTypePlugin = createApplicationPlugin(
    name = "ServerTypePlugin",
    createConfiguration = ::UpdateServerTypePluginConfig
) {
    onCall { call ->
        val path = call.request.uri

        println("Processing a request to $path")

        if (!path.startsWith("/update")) return@onCall

        val selfType = pluginConfig.type.toString().lowercase()

        if (!path.startsWith("/update/$selfType")) {
            call.respond(HttpStatusCode.BadRequest, UpdateError("This server is ${selfType.uppercase()}."))
            call.attributes.put(invalidTypeKey, true)
        } else {
            call.attributes.put(invalidTypeKey, false)
        }
    }
}
