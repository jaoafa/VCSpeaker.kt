package com.jaoafa.vcspeaker.api.update

import com.jaoafa.vcspeaker.api.update.ReloadModule.Companion.ReloaderJson
import com.jaoafa.vcspeaker.reload.ReloadServerCredential
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

enum class UpdateServerType {
    Latest, Current, Unknown
}

/**
 * VCSpeaker の更新用 API サーバーを表すクラスです。
 *
 * @property type [UpdateServerType]
 * @property targetCredential [type] が [UpdateServerType.Latest] の場合、[UpdateServerType.Current] サーバーの認証情報
 */
class UpdateServer(
    val type: UpdateServerType,
    var targetCredential: ReloadServerCredential? = null,
    val onAckCalled: () -> Unit = {},
    val onReadyCalled: () -> Unit = {}
) {
    private val logger = KotlinLogging.logger {}

    val selfCredential = ReloadServerCredential.generate()

    var targetPort = 0

    val client = HttpClient(io.ktor.client.engine.cio.CIO) {
        install(ContentNegotiation) {
            json(ReloaderJson)
        }
    }

    // todo: add timeout for each request (to prevent stucking)
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    /**
     * API サーバーを起動します。
     *
     * @param port バインドするポート番号。
     * @param wait 起動後にサスペンドするかどうか。
     */
    fun start(port: Int, wait: Boolean = false, sendBackInitSignal: Boolean = false) {
        logger.info { "Starting Update API server..." }

        // rotate the port between 2000 and 2001
        targetPort = if (port == 2000) port + 1 else port - 1

        logger.info { "Initiating a server as $type instance. ${selfCredential.id} [$port] <----> [$targetPort] ${targetCredential?.id}" }

        val reloadModule = ReloadModule(
            type, selfCredential, targetCredential, targetPort,
            sendBackInitSignal, onAckCalled = onAckCalled, onReadyCalled = onReadyCalled
        )

        val server = embeddedServer(CIO, port = port) {
            with(reloadModule) {
                module()
            }

            monitor.subscribe(ServerReady) {
                logger.info { "Update API server is ready at port $port" }
            }
        }

        this.server = server
        server.start(wait)
        logger.info { "Server started." }
    }

    suspend fun stopSuspend() {
        logger.info { "Stopping Update API server..." }
        server?.stopSuspend(1000, 1000)
        server = null
        logger.info { "Server stopped." }
    }
}
