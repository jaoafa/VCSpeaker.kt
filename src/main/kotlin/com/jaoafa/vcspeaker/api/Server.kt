package com.jaoafa.vcspeaker.api

import com.jaoafa.vcspeaker.KordStarter
import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.types.InitFinishedRequest
import com.jaoafa.vcspeaker.api.types.UpdateError
import com.jaoafa.vcspeaker.reload.Reload
import com.jaoafa.vcspeaker.reload.UpdateRequest
import com.jaoafa.vcspeaker.reload.state.State
import com.jaoafa.vcspeaker.reload.state.StateManager
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext
import com.sun.org.apache.xpath.internal.operations.Bool
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
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
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.system.exitProcess

enum class ServerType {
    Latest, Current, Unknown
}

/**
 * VCSpeaker の API サーバーを表すクラスです。
 *
 * @property type [ServerType]
 * @property targetToken [type] が [ServerType.Latest] の場合、[ServerType.Current] サーバーの認証トークン
 * @property targetId [type] が [ServerType.Latest] の場合、[ServerType.Current] の ID
 */
class Server(val type: ServerType, var targetToken: String? = null, var targetId: String? = null) {
    private val logger = KotlinLogging.logger {}

    val reloaderJsonFormat = Json {
        explicitNulls = false
        serializersModule = SerializersModule {
            polymorphic(ProviderContext::class) {
                subclass(SoundmojiContext::class)
                subclass(VoiceTextContext::class)
            }
        }
    }

    var sequence = 0
        private set

    private var lastUpdate = 0L

    val selfId = Reload.serverIds.random()

    @OptIn(ExperimentalEncodingApi::class)
    val selfToken = run {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        Base64.encode(bytes)
    }

    var targetPort = 0

    val client = HttpClient(io.ktor.client.engine.cio.CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(reloaderJsonFormat)
        }
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
     * @param route API のルート。頭の / は不要です。
     * @param data 任意の request body
     * @return Response body
     */
    suspend inline fun <reified T> requestUpdate(
        route: String,
        data: T,
        serializer: KSerializer<T>
    ) {
        val url = "http://localhost:$targetPort/$route"

        val response = client.post(
            url,
            requesterConfig(
                reloaderJsonFormat.encodeToString(
                    UpdateRequest.serializer(serializer),
                    UpdateRequest(sequence, data)
                )
            )
        )


        if (response.status != HttpStatusCode.OK) {
            val error = response.body<UpdateError>()
            throw IOException("Failed to request update: ${response.status} ${error.message}")
        }

        return
    }

    /**
     * ルートで受信した UpdateRequest をデコードします。
     *
     * @param serializer Body のシリアライザ
     * @return UpdateRequest<T>
     */
    suspend fun <T> RoutingCall.receiveUpdateOf(serializer: KSerializer<T>): UpdateRequest<T> {
        val response = reloaderJsonFormat
            .decodeFromString(UpdateRequest.serializer(serializer), receiveText())
        return response
    }

    /**
     * リクエストに肯定応答を返し、シーケンス番号を更新します。
     *
     * @param s シーケンス番号
     */
    private suspend fun RoutingCall.ok(s: Int) {
        respond(HttpStatusCode.OK)
        logger.info { "[S$s] Done." }
        sequence = s + 1
    }

    // todo: add timeout for each request (to prevent stucking)

    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    /**
     * API サーバーを起動します。
     *
     * @param port バインドするポート番号。
     * @param wait 起動後にサスペンドするかどうか。
     */
    fun start(port: Int, wait: Boolean = false) {
        // rotate the port between 2000 and 2001
        targetPort = if (port == 2000) port + 1 else port - 1

        logger.info { "Initiating a server as $type instance. $selfId [$port] <----> [$targetPort] $targetId" }

        val server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(reloaderJsonFormat)
            }

            install(ServerTypePlugin) {
                type = this@Server.type
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
                        } else null
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
                            /**
                             * S0 - Latest から Current へ、初期化が完了したことを通知します。
                             * Body: UpdateRequest<InitFinishedRequest>
                             */
                            post("/init-finished") {
                                if (call.attributes[invalidTypeKey]) return@post

                                val (s, request) = call.receiveUpdateOf(InitFinishedRequest.serializer())

                                if (s != 0) {
                                    call.respond(HttpStatusCode.BadRequest, UpdateError("Sequence mismatch."))
                                    return@post
                                }

                                targetId = request.id
                                targetToken = request.token

                                call.ok(s)

                                logger.info { "[S$sequence] $targetId has finished init. Transferring the state..." }

                                try {
                                    requestUpdate(
                                        "update/latest/state",
                                        State.generate(),
                                        State.serializer()
                                    )
                                } catch (e: Exception) {
                                    logger.error(e) { "[S$sequence] Failed to transfer the state. Aborting update." }
                                    return@post
                                }
                            }

                            /**
                             * S2 - Latest から Current へ、ステート同期とログイン準備が完了したことを通知します。
                             * Current は S3 - Ack を送信後に code 0 で終了します。
                             * Body: UpdateRequest<Boolean>
                             */
                            post("/ready") {
                                if (call.attributes[invalidTypeKey]) return@post

                                val (s, _) = call.receiveUpdateOf(Boolean.serializer())

                                if (s != 2) {
                                    call.respond(HttpStatusCode.BadRequest, UpdateError("Sequence mismatch."))
                                    return@post
                                }

                                call.ok(s)

                                logger.info { "[S$sequence] $targetId is ready. Exiting..." }

                                try {
                                    requestUpdate(
                                        "update/latest/ack",
                                        true,
                                        Boolean.serializer()
                                    )
                                } catch (e: Exception) {
                                    logger.error(e) { "[S$sequence] Failed to send ack signal. Aborting update. Exit cancelled." }
                                    return@post
                                }

                                VCSpeaker.removeShutdownHook()
                                exitProcess(0)
                            }
                        }

                        route("/latest") {
                            /**
                             * S1 - Current から Latest へ、[State] を転送します。
                             * Body: UpdateRequest<State>
                             */
                            post("/state") {
                                if (call.attributes[invalidTypeKey]) return@post

                                val (s, state) = call.receiveUpdateOf(State.serializer())

                                if (s != 1) {
                                    call.respond(HttpStatusCode.BadRequest, UpdateError("Sequence mismatch."))
                                    return@post
                                }

                                logger.info { "[S$sequence] $targetId has frozen the state. Restoring..." }

                                StateManager.restore(state)

                                call.ok(s)

                                try {
                                    requestUpdate(
                                        "update/current/ready",
                                        true,
                                        Boolean.serializer()
                                    )
                                } catch (e: Exception) {
                                    logger.error(e) { "[S$sequence] Failed to send ready signal. Aborting update." }
                                    return@post
                                }
                            }

                            /**
                             * S3 - Current から Latest へ、肯定応答を送信し、Latest はログインを開始します。
                             * Body: UpdateRequest<State>
                             */
                            post("/ack") {
                                if (call.attributes[invalidTypeKey]) return@post

                                val (s, _) = call.receiveUpdateOf(Boolean.serializer())

                                if (s != 3) {
                                    call.respond(HttpStatusCode.BadRequest, UpdateError("Sequence mismatch."))
                                    return@post
                                }

                                call.ok(s)

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

        this.server = server
        server.start(wait)
    }

    fun stop() {
        logger.info { "Stopping API server..." }
        runBlocking {
            server?.stopSuspend(1000, 1000)
        }
        server = null
        logger.info { "Server stopped." }
    }
}