package com.jaoafa.vcspeaker.reload

import com.jaoafa.vcspeaker.Options
import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.Server
import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.tools.VCSpeakerUserAgent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.time.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.IOException
import java.io.File
import java.net.ServerSocket

@Serializable
data class GitHubAsset(
    @SerialName("browser_download_url")
    val browserDownloadURL: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("name")
    val name: String,
)

@Serializable
data class GitHubRelease(
    @SerialName("html_url")
    val htmlURL: String,
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("assets")
    val assets: List<GitHubAsset>,
    val body: String
)

object Reload {
    private val logger = KotlinLogging.logger { }

    val serverIds = listOf(
        "apple",
        "banana",
        "orange",
        "grape",
        "strawberry",
        "pear",
        "peach",
        "cherry",
        "lemon",
        "blueberry"
    )

    private val client = HttpClient(CIO) {
        VCSpeakerUserAgent()

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 100000
        }
    }

    /**
     * GitHub Releases から最新のリリースを取得し、更新があればダウンロードします。
     * jar は ./updates に保存されます。
     *
     * @throws IllegalStateException GitHub Releases から jar が見つからなかった場合
     * @return ダウンロードした [File]; 更新がなければ null を返します。
     */
    suspend fun checkUpdate(): File? {
        val repo = VCSpeaker.config[EnvSpec.updateRepo]
        val url = "https://api.github.com/repos/$repo/releases/latest"

        logger.info { "Checking update from $repo" }

        val response = client.get(url)
        val release = response.body<GitHubRelease>()

        // tagName ... v1.2.3
        // version ... 1.2.3
        if (release.tagName.removePrefix("v") == VCSpeaker.version) {
            logger.info { "Already up-to-date. VCSpeaker ${release.tagName}" }
            return null
        } else {
            logger.info { "Update found! VCSpeaker ${release.tagName} (v${VCSpeaker.version} on local)" }
        }

        val asset = release.assets.firstOrNull { it.contentType == "application/java-archive" }
            ?: throw IllegalStateException("No jar found in release ${release.tagName}")

        var lastProgressUpdate = System.currentTimeMillis()

        val jarResponse = client.get(asset.browserDownloadURL) {
            onDownload { bytesSentTotal, contentLength ->
                if (System.currentTimeMillis() - lastProgressUpdate < 1000) return@onDownload

                val percent = if (contentLength != null) {
                    ((bytesSentTotal.toDouble() / contentLength) * 100).toInt()
                } else {
                    0
                }

                lastProgressUpdate = System.currentTimeMillis()

                logger.info { "Downloading ${asset.name}. $bytesSentTotal/$contentLength bytes received ($percent%)" }
            }
        }

        if (!File("./updates").exists())
            File("./updates").mkdirs()

        val jar = File("./updates/${asset.name}")
        jar.writeBytes(jarResponse.body())

        return jar
    }

    fun updateTo(jar: File) {
        logger.info { "Updating to ${jar.name}..." }

        val updateJar = jar.copyTo(File("./update-${System.currentTimeMillis()}.jar"), overwrite = true)

        val port = listOf(2000, 2001).first {
            try {
                ServerSocket(it).use { true }
            } catch (e: IOException) {
                logger.info { "Port $it is already in use." }
                false
            }
        }

        ProcessBuilder(
            "nohup", "java", "-jar", updateJar.absolutePath,
            "--api-port", port.toString(),
            "--wait-for", Server.selfId.toString(),
            "--api-token", Server.selfToken,
        ).redirectOutput(File("./update.log"))
            .redirectError(File("./update.log"))
            .start()

        logger.info { "Update launched." }
    }
}