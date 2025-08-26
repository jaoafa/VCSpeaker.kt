package com.jaoafa.vcspeaker.reload

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.Server
import com.jaoafa.vcspeaker.api.ServerType
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Timer
import java.util.TimerTask

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

    var prodUpdateInDevWarned = false

    fun shouldContinueUpdate(nextVersion: String, bypassDevLock: Boolean = false): Boolean {
        // tagName ... v1.2.3
        // version ... 1.2.3
        if (nextVersion == VCSpeaker.version) { // up to date
            logger.info { "Already up-to-date. VCSpeaker v${nextVersion}" }
            return false
        } else if (VCSpeaker.isDev()) { // tags don't match, but VCSpeaker is in dev mode
            if (!bypassDevLock && !prodUpdateInDevWarned) { // no bypassDevLock, warn only once
                logger.warn { "Production Update Found. To simulate update process, use \"/update bypassdevlock\" (v${VCSpeaker.version} on local)" }

                prodUpdateInDevWarned = true
                return false
            } else if (!bypassDevLock) { // no bypassDevLock
                return false
            } else { // bypassDevLock
                logger.info { "Bypassing production update lock in dev mode..." }
                logger.info { "Updating to VCSpeaker v${nextVersion} (v${VCSpeaker.version} on local)" }
            }
        } else {
            logger.info { "Update found! VCSpeaker v${nextVersion} (v${VCSpeaker.version} on local)" }
        }

        return true
    }

    /**
     * GitHub Releases から最新のリリースを取得し、更新があればダウンロードします。
     * jar archive は ./updates に保存されます。
     *
     * @throws IllegalStateException GitHub Releases から jar archive が見つからなかった場合
     * @return ダウンロードした [File]; 更新がなければ null を返します。
     */
    suspend fun downloadUpdate(bypassDevLock: Boolean = false): File? {
        val repo = VCSpeaker.config[EnvSpec.updateRepo]
        val url = "https://api.github.com/repos/$repo/releases/latest"

        logger.info { "Checking update from $repo" }

        val response = client.get(url)
        val release = response.body<GitHubRelease>()

        val shouldContinue = shouldContinueUpdate(release.tagName.removePrefix("v"), bypassDevLock)
        if (!shouldContinue) return null

        val asset = release.assets.firstOrNull { it.contentType == "application/java-archive" }
            ?: throw IllegalStateException("No jar found in release ${release.tagName}")

        val jar = File("./updates/${asset.name}")

        if (jar.exists()) {
            logger.info { "Found existing jar file: ${jar.absolutePath}. Not downloading." }
            return jar
        }

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

        jar.writeBytes(jarResponse.body())

        return jar
    }

    /**
     * 指定された jar archive を使用して VCSpeaker を更新します。
     * 更新後は新しいプロセスが起動し、現在のプロセスは終了します。
     *
     * @param jar 更新先の jar archive
     */
    fun updateTo(jar: File) {
        logger.info { "Updating to ${jar.name}..." }

        // copy to the current working directory
        val updateJar = jar.copyTo(File("./update-${System.currentTimeMillis()}.jar"), overwrite = true)

        val server = Server(ServerType.Current)
        VCSpeaker.apiServer?.stop()
        VCSpeaker.apiServer = server
        server.start(2000)

        ProcessBuilder(
            "nohup", "java", "-jar", updateJar.absolutePath,
            "--api-port", "2001",
            "--wait-for", server.selfId,
            "--api-token", server.selfToken,
        ).redirectOutput(File("./update.log"))
            .redirectError(File("./update.log"))
            .start()

        logger.info { "Update launched." }
    }

    /**
     * autoUpdate が true の場合に、自動更新を初期化します。
     * 5 分ごとに更新をチェックし、更新があれば自動的に更新を行います。
     */
    fun initAutoUpdate() {
        if (!(VCSpeaker.options.autoUpdate ?: VCSpeaker.config[EnvSpec.autoUpdate])) {
            logger.info { "Automatic update is disabled. To turn it on, start VCSpeaker with --auto-update flag." }
            return
        }

        Timer().schedule(object : TimerTask() {
            override fun run() {
                try {
                    logger.info { "Auto update is enabled. Checking for updates..." }

                    val jar = runBlocking { downloadUpdate() }

                    if (jar != null) {
                        updateTo(jar)
                    } else {
                        logger.info { "Not updating." }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to check for updates." }
                }
            }
        }, 0, 5 * 60 * 1000)
    }
}