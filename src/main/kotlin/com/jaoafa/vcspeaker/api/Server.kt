package com.jaoafa.vcspeaker.api

import com.jaoafa.vcspeaker.KordStarter
import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.types.InitFinishedRequest
import com.jaoafa.vcspeaker.reload.state.State
import com.jaoafa.vcspeaker.reload.state.StateManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.system.exitProcess

enum class ServerType {
    Latest, Current, Unknown
}

object Server {
    private val logger = KotlinLogging.logger {}

    val selfId = System.currentTimeMillis()
    var targetId: Long? = null

    var targetPort = 0
    var targetToken: String? = VCSpeaker.options.apiToken

    @OptIn(ExperimentalEncodingApi::class)
    val selfToken = run {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        Base64.encode(bytes)
    }

    var type = ServerType.Unknown

    val client = HttpClient(io.ktor.client.engine.cio.CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }

    enum class RequestType {
        Get, Post
    }

    fun requesterConfig(body: Any? = null): HttpRequestBuilder.() -> Unit = {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)

        basicAuth(
            username = selfId.toString(),
            password = targetToken ?: throw Exception("targetToken is null")
        )

        if (body != null) setBody(body)
    }

    /**
     * VCSpeaker の内部 API にリクエストを送信します。
     *
     * @param T Request body の型
     * @param R Response body の型
     * @param type HTTP request の method
     * @param route API のルート。頭の / は不要です。
     * @param body 任意の request body
     * @return Response body
     */
    suspend inline fun <reified T, reified R> request(type: RequestType, route: String, body: T?): R {
        val url = "http://localhost:$targetPort/$route"

        val response = when (type) {
            RequestType.Get -> client.get(url, requesterConfig())
            RequestType.Post -> client.post(url, requesterConfig(body))
        }

        return response.body<R>()
    }

    fun start(port: Int, wait: Boolean = false) {
        targetPort = if (port == 2000) port + 1 else port - 1

        val waitFor = VCSpeaker.options.waitFor

        if (waitFor != null) {
            type = ServerType.Latest
            targetId = waitFor
            logger.info { "Launching $selfId as LATEST instance. Waiting for $waitFor..." }
        } else {
            type = ServerType.Current
            logger.info { "Launching $selfId as CURRENT instance." }
        }

        val server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json()
            }

            install(ServerTypePlugin) {
                type = Server.type
            }

            authentication {
                basic("update-basic-auth") {
                    realm = "VCSpeaker Updater API"
                    validate { credentials ->
                        val namePass = if (targetId == null) true else { // Accept username only once
                            credentials.name == targetId.toString()
                        }

                        if (namePass && credentials.password == selfToken) {
                            UserIdPrincipal(credentials.name)
                        } else {
                            null
                        }
                    }
                }
            }

            routing {
                get("/version") {
                    call.respond(HttpStatusCode.OK, VCSpeaker.version)
                }

                route("/update") {
                    authenticate("update-basic-auth") {
                        route("/current") {
                            post("/init-finished") { // 1: L -> C, L finished init
                                if (call.attributes[invalidTypeKey]) return@post

                                val (token, id) = call.receive<InitFinishedRequest>()

                                targetToken = token
                                targetId = id

                                call.respond(HttpStatusCode.OK)

                                logger.info { "LATEST has finished init. Transferring the state..." }

                                request<State, Unit>(RequestType.Post, "update/latest/state", State.generate())
                            }

                            get("/ready") { // 3: L -> C, L ready
                                if (call.attributes[invalidTypeKey]) return@get

                                call.respond(HttpStatusCode.OK)

                                request<Unit, Unit>(RequestType.Get, "update/latest/ack", null)
                                logger.info { "Exiting..." }

                                Runtime.getRuntime().halt(0)
                            }
                        }
                        route("/latest") {
                            post("/state") { // 2: C -> L, C froze state
                                if (call.attributes[invalidTypeKey]) return@post

                                logger.info { "CURRENT has frozen the state. Accepting the state..." }

                                val state = call.receive<State>()

                                StateManager.restore(state)

                                call.respond(HttpStatusCode.OK)

                                request<Unit, Unit>(RequestType.Get, "update/current/ready", null)
                            }

                            get("/ack") { // 4: C -> L, C exit
                                if (call.attributes[invalidTypeKey]) return@get

                                call.respond(HttpStatusCode.OK)

                                logger.info { "Logging in..." }

                                val instance = KordStarter.instance

                                if (instance != null) {
                                    instance.start()
                                } else {
                                    throw IllegalStateException("Unexpected error: KordEx instance is null.")
                                }
                            }
                        }
                    }

                }
            }
        }

        server.start(wait)
    }
}