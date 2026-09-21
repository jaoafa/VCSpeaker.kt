package api.update

import com.jaoafa.vcspeaker.KordStarter
import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.update.ReloadModule
import com.jaoafa.vcspeaker.api.update.ReloadModule.Companion.ReloaderJson
import com.jaoafa.vcspeaker.api.update.UpdateServerType
import com.jaoafa.vcspeaker.reload.ReloadServerCredential
import com.jaoafa.vcspeaker.reload.UpdateRequest
import com.jaoafa.vcspeaker.reload.state.State
import com.jaoafa.vcspeaker.reload.state.StateManager
import dev.kordex.core.ExtensibleBot
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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify

class ReloadModuleLatestTest : FunSpec({
    val selfCredential = ReloadServerCredential(id = "latest-server", token = "latest-token-456")
    val currentCredential = ReloadServerCredential(id = "current-server", token = "current-token-123")
    val targetPort = 2000

    val mockBot = mockk<ExtensibleBot>(relaxed = true)

    fun createReloadModule(
        client: HttpClient? = null
    ) = ReloadModule(
        type = UpdateServerType.Latest,
        selfCredential = selfCredential,
        providedTargetCredential = currentCredential,
        targetPort = targetPort,
        sendBackInitSignal = false,
        client = client
    )

    beforeSpec {
        mockkObject(VCSpeaker)
        mockkObject(StateManager)
        mockkObject(KordStarter)

        every { VCSpeaker.version } returns "0.0.0-test"
        every { StateManager.restore(any()) } just Runs
        every { KordStarter.instance } returns mockBot
        coEvery { mockBot.start() } just Runs
    }

    afterSpec {
        unmockkAll()
    }

    test("Return 401 Unauthorised if invalid credentials are provided") {
        val module = createReloadModule()
        testApplication {
            application { with(module) { module() } }

            val response = client.post("/update/latest/state") {
                basicAuth("invalid-id", "invalid-token")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("Return 400 Bad Request if the requested server type is invalid") {
        val module = createReloadModule()
        testApplication {
            application { with(module) { module() } }

            val response = client.post("/update/current/init-finished") {
                basicAuth(currentCredential.id, selfCredential.token)
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("Return 400 Bad Request if the sequence number is invalid") {
        val module = createReloadModule()
        testApplication {
            application { with(module) { module() } }

            val state = State(args = arrayOf("--test"), narrators = emptyList())
            val requestBody = ReloaderJson.encodeToString(
                UpdateRequest(sequence = 99, data = state)
            )

            val response = client.post("/update/latest/state") {
                contentType(ContentType.Application.Json)
                basicAuth(currentCredential.id, selfCredential.token)
                setBody(requestBody)
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("S1 /state endpoint responds OK, restores state, and requests S2 /ready") {
        var readyReceived = false

        testApplication {
            val latestClient = createClient {
                install(ContentNegotiation) {
                    json(ReloaderJson)
                }
            }

            val module = createReloadModule(client = latestClient)

            application {
                with(module) { module() }
            }

            externalServices { // mock Current's /ready endpoint
                hosts("http://localhost:$targetPort") {
                    routing {
                        post("/update/current/ready") {
                            readyReceived = true
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
            }

            val state = State(args = arrayOf("--test"), narrators = emptyList())
            val requestBody = ReloaderJson.encodeToString(
                UpdateRequest(sequence = 1, data = state)
            )

            val response = client.post("/update/latest/state") {
                contentType(ContentType.Application.Json)
                basicAuth(currentCredential.id, selfCredential.token)
                setBody(requestBody)
            }

            response.status shouldBe HttpStatusCode.OK
            module.sequence shouldBe 2
            verify(exactly = 1) {
                StateManager.restore(match {
                    it.args.contentEquals(state.args) && it.narrators == state.narrators
                })
            }
        }

        readyReceived shouldBe true
    }

    test("S3 /ack endpoint responds OK, and starts Kord instance") {
        val module = createReloadModule()
        testApplication {
            application { with(module) { module() } }

            val requestBody = ReloaderJson.encodeToString(
                UpdateRequest(sequence = 3, data = true)
            )

            val response = client.post("/update/latest/ack") {
                contentType(ContentType.Application.Json)
                basicAuth(currentCredential.id, selfCredential.token)
                setBody(requestBody)
            }

            response.status shouldBe HttpStatusCode.OK
            module.sequence shouldBe 4
            coVerify(exactly = 1) { mockBot.start() }
        }
    }
})
