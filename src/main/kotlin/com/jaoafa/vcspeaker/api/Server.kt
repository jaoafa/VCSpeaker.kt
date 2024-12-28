package com.jaoafa.vcspeaker.api

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.system.exitProcess

class Server {
    fun start(port: Int, wait: Boolean) {
        embeddedServer(Netty, port) {
            routing {
                get("/") {
                    call.respondText("Hello, world!")
                }
                route("/update") {
                    post("/state") {

                    }
                    get("ready") {

                        exitProcess(0)
                    }
                    get("ack") {

                    }
                }
            }
        }.start(wait)
    }
}