package com.jaoafa.vcspeaker.api.update

import com.jaoafa.vcspeaker.api.update.modules.ReloadModule
import com.jaoafa.vcspeaker.reload.Reload
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

enum class UpdateServerType {
    Latest, Current, Unknown
}

/**
 * VCSpeaker の更新用 API サーバーを表すクラスです。
 *
 * @property type [UpdateServerType]
 * @property targetToken [type] が [UpdateServerType.Latest] の場合、[UpdateServerType.Current] サーバーの認証トークン
 * @property targetId [type] が [UpdateServerType.Latest] の場合、[UpdateServerType.Current] の ID
 */
class UpdateServer(val type: UpdateServerType, var targetToken: String? = null, var targetId: String? = null) {
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
        install(ContentNegotiation) {
            json(reloaderJsonFormat)
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
    fun start(port: Int, wait: Boolean = false, sendBackIntSignal: Boolean = false) {
        logger.info { "Starting Update API server..." }

        // rotate the port between 2000 and 2001
        targetPort = if (port == 2000) port + 1 else port - 1

        logger.info { "Initiating a server as $type instance. $selfId [$port] <----> [$targetPort] $targetId" }

        val reloadModule = ReloadModule(type, targetToken, targetId, sendBackIntSignal)

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
