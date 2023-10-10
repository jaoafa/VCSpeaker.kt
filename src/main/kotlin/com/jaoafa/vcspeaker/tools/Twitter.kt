package com.jaoafa.vcspeaker.tools

import com.jaoafa.vcspeaker.models.original.Tweet
import com.jaoafa.vcspeaker.models.response.twitter.TwitterOembedResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import net.htmlparser.jericho.Source

object Twitter {
    private const val baseURL = "https://publish.twitter.com/oembed"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun getTweet(screenName: String, tweetId: String): Tweet? {
        val tweetUrl = "https://twitter.com/$screenName/status/$tweetId"
        val response = client.get(baseURL) {
            parameter("url", tweetUrl)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val json: TwitterOembedResponse = response.body()
                Tweet(
                    json.authorName,
                    json.html,
                    Source(json.html.replace("<a.*>(.*)</a>", ""))
                        .getFirstElement("p")
                        .renderer
                        .setMaxLineLength(Integer.MAX_VALUE)
                        .setNewLine(null)
                        .toString()
                )
            }

            else -> null
        }
    }
}