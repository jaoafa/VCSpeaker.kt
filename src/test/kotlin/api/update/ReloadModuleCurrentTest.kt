package api.update

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.update.ReloadModule
import com.jaoafa.vcspeaker.api.update.ReloadModule.Companion.ReloaderJson
import com.jaoafa.vcspeaker.api.update.UpdateServerType
import com.jaoafa.vcspeaker.reload.ReloadServerCredential
import com.jaoafa.vcspeaker.reload.UpdateRequest
import com.jaoafa.vcspeaker.reload.state.State
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll

class ReloadModuleCurrentTest : FunSpec({
    val selfCredential = ReloadServerCredential(id = "current-server", token = "current-token-123")
    val latestCredential = ReloadServerCredential(id = "latest-server", token = "latest-token-456")
    val targetPort = 2001

    fun createReloadModule(
        exitHandler: (Int) -> Unit = { throw Exception("exitHandler Called") },
        client: HttpClient? = null
    ) = ReloadModule(
        type = UpdateServerType.Current,
        selfCredential = selfCredential,
        providedTargetCredential = latestCredential,
        targetPort = targetPort,
        sendBackInitSignal = false,
        exitHandler = exitHandler,
        client = client
    )

    beforeSpec {
        mockkObject(VCSpeaker)
        mockkObject(State)
        every { VCSpeaker.version } returns "0.0.0-test"
        every { VCSpeaker.removeShutdownHook() } returns true
    }

    afterSpec {
        unmockkAll()
    }

    test("Return 401 Unauthorised if invalid credentials are provided") {
        val module = createReloadModule()
        testApplication {
            application { with(module) { module() } }

            val response = client.post("/update/current/init-finished") {
                basicAuth("invalid-id", "invalid-token")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("Return 400 Bad Request if the requested server type is invalid") {
        val module = createReloadModule()
        testApplication {
            application { with(module) { module() } }

            val response = client.post("/update/latest/state") {
                basicAuth(latestCredential.id, selfCredential.token)
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("Return 400 Bad Request if the sequence number is invalid") {
        val module = createReloadModule()
        testApplication {
            application { with(module) { module() } }

            val requestBody = ReloaderJson.encodeToString(
                UpdateRequest(sequence = 99, data = latestCredential)
            )

            val response = client.post("/update/current/init-finished") {
                contentType(ContentType.Application.Json)
                basicAuth(latestCredential.id, selfCredential.token)
                setBody(requestBody)
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("S0 /init-finished endpoint responds OK, updating the sequence number") {
        val module = createReloadModule()
        testApplication {
            application { with(module) { module() } }

            val requestBody = ReloaderJson.encodeToString(
                UpdateRequest(sequence = 0, data = latestCredential)
            )

            val response = client.post("/update/current/init-finished") {
                contentType(ContentType.Application.Json)
                basicAuth(latestCredential.id, selfCredential.token)
                setBody(requestBody)
            }

            response.status shouldBe HttpStatusCode.OK
            module.sequence shouldBe 1
            module.targetCredential shouldBe latestCredential
        }
    }

    test("S2 /ready endpoint responds OK, and exits process") {
        var exitCode: Int? = null
        var ackReceived = false

        testApplication {
            val currentClient = createClient {
                install(ContentNegotiation) {
                    json(ReloaderJson)
                }
            }

            val module = createReloadModule(
                exitHandler = { code -> exitCode = code },
                client = currentClient
            )

            application {
                with(module) { module() }
            }

            externalServices { // mock Latest's /ack endpoint
                hosts("http://localhost:$targetPort") {
                    routing {
                        post("/update/latest/ack") {
                            ackReceived = true
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
            }

            val requestBody = ReloaderJson.encodeToString(
                UpdateRequest(sequence = 2, data = true)
            )

            val response = client.post("/update/current/ready") {
                contentType(ContentType.Application.Json)
                basicAuth(latestCredential.id, selfCredential.token)
                setBody(requestBody)
            }

            response.status shouldBe HttpStatusCode.OK
        }

        ackReceived shouldBe true
        exitCode shouldBe 0
    }
})
