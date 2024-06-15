package com.jaoafa.vcspeaker.tools

import com.jaoafa.vcspeaker.models.original.twitter.Tweet
import com.jaoafa.vcspeaker.models.response.twitter.TwitterOEmbedResponse
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
    private const val BASE_URL = "https://publish.twitter.com/oembed"

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
        val response = client.get(BASE_URL) {
            parameter("url", tweetUrl)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val json: TwitterOEmbedResponse = response.body()
                val plainText = Source(json.html.replace("<a.*>(.*)</a>", ""))
                    .getFirstElement("p")
                    .renderer
                    .setMaxLineLength(Integer.MAX_VALUE)
                    .setNewLine(null)
                    .toString()
                val readText = getReadText(plainText).trim()
                Tweet(
                    json.authorName,
                    json.html,
                    plainText,
                    readText
                )
            }

            else -> null
        }
    }

    private fun getReadText(text: String): String {
        // "#(\S+)" は " ハッシュタグ「$1」 " に変換
        // t.co, twitter.com, pic.twitter.com のリンクを削除。プロトコル有無両方に対応
        return text
            .replace("#(\\S+)".toRegex(), " ハッシュタグ「$1」 ")
            .replace("<?(?:https?://)?t\\.co/[a-zA-Z0-9]+>?".toRegex(), "")
            .replace("<?(?:https?://)?pic\\.(?:x|twitter)\\.com/[a-zA-Z0-9]+>?".toRegex(), "")
            .replace("<?(?:https?://)?(?:x|twitter)\\.com/.+>?".toRegex(), "")
    }
}