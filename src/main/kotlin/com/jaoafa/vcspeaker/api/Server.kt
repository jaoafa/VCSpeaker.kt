package com.jaoafa.vcspeaker.api

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.types.Error
import com.jaoafa.vcspeaker.state.State
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.system.exitProcess

enum class ServerType {
    Latest, Current, Unknown
}

object Server {
    private val logger = KotlinLogging.logger {}

    val selfId = System.currentTimeMillis()
    var latestId: Long? = null
    var currentId: Long? = null

    var otherPort = 0

    fun type() = when {
        latestId == null && currentId == null -> ServerType.Unknown
        latestId != null && currentId == null -> ServerType.Latest
        latestId == null && currentId != null -> ServerType.Current
        else -> ServerType.Unknown
    }

    val client = HttpClient(io.ktor.client.engine.cio.CIO)

    enum class RequestType {
        Get, Post
    }

    suspend inline fun <reified T, reified R> request(type: RequestType, route: String, body: T?): R {
        val response = when (type) {
            RequestType.Get -> {
                client.get("http://localhost:$otherPort/$route") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                }
            }

            RequestType.Post -> {
                client.post("http://localhost:$otherPort/$route") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)

                    setBody(body)
                }
            }
        }

        return response.body<R>()
    }

    fun start(port: Int) {
        otherPort = if (port == 2000) port + 1 else port - 1

        val waitFor = VCSpeaker.options.waitFor

        if (waitFor != null) {
            currentId = waitFor
            logger.info { "Launching $selfId as LATEST instance. Waiting for $waitFor..." }
        } else {
            latestId = selfId
            logger.info { "Launching $selfId as CURRENT instance." }
        }

        val server = embeddedServer(CIO, port = port) {
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
                            if (type() == ServerType.Latest)
                                call.respond(HttpStatusCode.BadRequest, Error("This server is LATEST."))

                            val id = call.queryParameters["id"]

                            if (id == null) {
                                call.respond(HttpStatusCode.BadRequest, Error("ID is required."))
                            } else {
                                call.respond(HttpStatusCode.OK, id)
                            }

                            request<State, Unit>(RequestType.Post, "update/current/state", State.generate())
                        }

                        get("/ready") { // 3: L -> C, L ready
                            if (type() == ServerType.Latest)
                                call.respond(HttpStatusCode.BadRequest, Error("This server is LATEST."))

                            request<Unit, Unit>(RequestType.Get, "update/latest/ack", null)
                            exitProcess(0)
                        }
                    }
                    route("/latest") {
                        post("/state") { // 2: C -> L, C froze state
                            if (type() == ServerType.Current)
                                call.respond(HttpStatusCode.BadRequest, Error("This server is CURRENT."))

                            val state = call.receive<State>()
                            // Restore state

                            request<Unit, Unit>(RequestType.Get, "update/current/ready", null)
                        }

                        get("/ack") { // 4: C -> L, C exit
                            if (type() == ServerType.Current)
                                call.respond(HttpStatusCode.BadRequest, Error("This server is CURRENT."))


                        }
                    }
                }
            }
        }

        server.start(wait = false)
    }
}