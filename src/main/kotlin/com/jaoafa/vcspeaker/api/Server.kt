package com.jaoafa.vcspeaker.api

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.types.Error
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.system.exitProcess

enum class ServerType {
    Latest, Current, Unknown
}

object Server {
    val selfId = System.currentTimeMillis()
    var latestId: Long? = null
    var currentId: Long? = null

    fun type() = when {
        latestId == null && currentId == null -> ServerType.Unknown
        latestId != null && selfId == null -> ServerType.Latest
        latestId == null && selfId != null -> ServerType.Current
        else -> ServerType.Unknown
    }

    fun start(port: Int) {
        embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json()
            }

            routing {
                route("/update") {
                    get("/version") {
                        call.respond(HttpStatusCode.OK, VCSpeaker.version)
                    }
                    route("/current") {
                        get("/init-finished") { // 1: L -> C, L finished init
                            val id = call.queryParameters["id"]

                            if (id == null) {
                                call.respond(HttpStatusCode.BadRequest, Error("ID is required."))
                            } else {
                                call.respond(HttpStatusCode.OK, id)
                            }
                        }

                        get("/ready") { // 3: L -> C, L ready

                            exitProcess(0)
                        }
                    }
                    route("/latest") {
                        post("/state") { // 2: C -> L, C froze state

                        }

                        get("/ack") { // 4: C -> L, C exit

                        }
                    }
                }
            }
        }.start()
    }
}