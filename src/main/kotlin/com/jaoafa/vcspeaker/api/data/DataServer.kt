package com.jaoafa.vcspeaker.api.data

import com.jaoafa.vcspeaker.database.SnappableEntity
import com.jaoafa.vcspeaker.database.tables.*
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.toSnowflake
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class DataServer {
    private val logger = KotlinLogging.logger {}

    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    fun <T : Any, E : Entity<T>> Application.dataModule(
        path: String,
        entityClass: EntityClass<T, E>,
        castId: String.() -> T
    ) {
        routing {
            route(path) {
                get {
                    val limit = call.request.queryParameters["limit"]?.toInt()

                    val entities = transaction {
                        entityClass.all().let {
                            if (limit != null && limit > 0) {
                                it.limit(limit)
                            } else it
                        }.toList()
                    }

                    val snapshots = entities.map {
                        if (it is SnappableEntity<*, *>) {
                            it.getSnapshot()
                        } else {
                            return@get call.respond(HttpStatusCode.NotImplemented)
                        }
                    }

                    call.respond(snapshots)
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.castId() ?: return@get call.respond(HttpStatusCode.BadRequest)

                    val entity = transaction { entityClass.findById(id) }
                    when (entity) {
                        is SnappableEntity<*, *> -> {
                            call.respond(entity.getSnapshot())
                        }

                        null -> {
                            call.respond(HttpStatusCode.NotFound)
                        }

                        else -> {
                            call.respond(HttpStatusCode.NotImplemented)
                        }
                    }
                }
            }
        }
    }

    fun start(port: Int, wait: Boolean = false) {
        logger.info { "Starting Data API server..." }

        val server = embeddedServer(CIO, port = port) {
            monitor.subscribe(ServerReady) {
                logger.info { "Data API server is ready at port $port" }
            }

            install(ContentNegotiation) {
                json()
            }

            dataModule("/alias", AliasEntity.Companion, String::toInt)
            dataModule("/guild", GuildEntity.Companion) { toLong().toSnowflake() }
            dataModule("/ignore", IgnoreEntity.Companion, String::toInt)
            dataModule("/readable_bot", ReadableBotEntity.Companion, String::toInt)
            dataModule("/readable_channel", ReadableChannelEntity.Companion, String::toInt)
            dataModule("/speech_cache", SpeechCacheEntity.Companion, String::toInt)
            dataModule("/user", UserEntity.Companion) { toLong().toSnowflake() }
            dataModule("/vc_title", VCTitleEntity.Companion, String::toInt)
            dataModule("/voice", VoiceEntity.Companion, String::toInt)

            // ID Format: YYYY-MM, e.g., 2026-04
            dataModule("/vision_api_counter", VisionAPICounterEntity.Companion) {
                val (year, month) = split("-").map(String::toInt)
                CompositeID {
                    it[VisionAPICounterTable.year] = year
                    it[VisionAPICounterTable.month] = month
                }
            }
        }

        this.server = server
        server.start(wait)
    }
}
