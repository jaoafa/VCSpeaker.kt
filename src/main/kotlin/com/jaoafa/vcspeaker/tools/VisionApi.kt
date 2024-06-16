package com.jaoafa.vcspeaker.tools

import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString
import com.google.rpc.Status
import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.VisionApiCounterStore
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.canvas.drawables.FilledRect
import com.sksamuel.scrimage.canvas.drawables.Text
import kotlinx.serialization.Serializable
import org.apache.commons.codec.digest.DigestUtils
import java.awt.Color
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection
import kotlin.math.roundToInt

@Serializable
data class VisionVertex(
    val x: Int,
    val y: Int,
)

@Serializable
data class VisionTextAnnotation(
    val description: String,
    val locale: String,
    val score: Float,
    val vertices: List<VisionVertex>,
)

object VisionApi {
    /**
     * Vision APIにリクエストを送信し、VisionTextAnnotationのリストを取得する。
     *
     * @throws VisionApiLimitExceededException 月のリクエスト数が上限に達している場合
     * @throws VisionApiUnsupportedMimeTypeException サポートされていない MIME タイプの ByteArray が指定された場合
     * @throws VisionApiErrorException Vision API でエラーが発生した場合
     */
    fun getTextAnnotations(binaryArray: ByteArray): List<VisionTextAnnotation> {
        // MimeTypeを確認し、対応しているか確認する
        val mimeType = binaryArray.getMimeType()
        if (mimeType !in setOf(
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/bmp"
            )
        ) {
            throw VisionApiUnsupportedMimeTypeException(mimeType)
        }

        val fileHash = DigestUtils.md5Hex(binaryArray)

        // キャッシュフォルダが存在しない場合は作成する
        if (!VCSpeaker.Files.visionApiCache.exists()) {
            VCSpeaker.Files.visionApiCache.mkdirs()
        }

        val cacheFile = VCSpeaker.Files.visionApiCache + File(fileHash)
        if (cacheFile.exists()) {
            // キャッシュが存在する場合はキャッシュを返す
            val response = AnnotateImageResponse.parseFrom(cacheFile.readBytes())
            return response.textAnnotationsList.map { it.convertVisionTextAnnotation() }
        }

        if (VisionApiCounterStore.isLimitExceeded()) {
            throw VisionApiLimitExceededException()
        }

        // see https://cloud.google.com/vision/docs/samples/vision-quickstart
        try {
            val proto = ByteString.copyFrom(binaryArray)
            val vision = ImageAnnotatorClient.create()
            val image = Image.newBuilder().setContent(proto).build()
            val feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build()
            val request = AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build()

            VisionApiCounterStore.increment()
            val responses = vision.batchAnnotateImages(listOf(request))
            vision.close()
            if (responses.responsesCount == 0) {
                throw Exception("No response")
            }
            val response = responses.getResponses(0)
            if (response.hasError()) {
                throw VisionApiErrorException(response.error)
            }
            // レスポンスの保存
            response.save(fileHash)

            return response.textAnnotationsList.map { it.convertVisionTextAnnotation() }
        } catch (e: Throwable) {
            throw VisionApiUnknownErrorException(e)
        }
    }

    /**
     * Vision APIのキャッシュデータをもとに、画像に対して文字位置を示す画像を生成する。
     */
    fun drawTextAnnotations(binaryArray: ByteArray): ImmutableImage {
        val fileHash = DigestUtils.md5Hex(binaryArray)
        val cacheFile = VCSpeaker.Files.visionApiCache + File(fileHash)
        if (!cacheFile.exists()) {
            throw Exception("Cache file not found")
        }

        val response = AnnotateImageResponse.parseFrom(cacheFile.readBytes())

        var canvas = ImmutableImage.loader().fromBytes(binaryArray).toCanvas()
        // 1つ目は全体のテキストなので除外
        for (textAnnotation in response.textAnnotationsList.drop(1)) {
            val vertices = textAnnotation.boundingPoly.verticesList
            val x1 = vertices[0].x
            val y1 = vertices[0].y
            val x2 = vertices[2].x
            val y2 = vertices[2].y

            // フォントサイズは、矩形の高さの 1/2 とする。ただし、最小値は 10 とする。
            var fontSize = (y2 - y1) / 2f
            if (fontSize < 10) {
                fontSize = 10f
            }
            canvas = canvas.draw(
                // 矩形を描画
                FilledRect(x1, y1, x2 - x1, y2 - y1) {
                    // グレーの半透明の矩形を描画
                    it.color = Color(0, 0, 0, 128)
                },
                // 文字列を描画
                Text(textAnnotation.description, x1, (y1 + fontSize).roundToInt()) {
                    it.color = Color.WHITE
                    it.font = it.font.deriveFont(fontSize)
                }
            )
        }

        return canvas.image
    }

    /** ByteArray から Mime Type を取得する */
    private fun ByteArray.getMimeType(): String {
        return URLConnection.guessContentTypeFromStream(ByteArrayInputStream(this)) ?: "application/octet-stream"
    }

    /** EntityAnnotation を VisionTextAnnotation に変換する */
    private fun EntityAnnotation.convertVisionTextAnnotation(): VisionTextAnnotation {
        val vertices = this.boundingPoly.verticesList.map { VisionVertex(it.x, it.y) }
        return VisionTextAnnotation(this.description, this.locale, this.score, vertices)
    }

    /** AnnotateImageResponse をキャッシュとしてファイルに保存する */
    private fun AnnotateImageResponse.save(fileHash: String) {
        val cacheFile = VCSpeaker.Files.visionApiCache + File(fileHash)
        cacheFile.writeBytes(this.toByteArray())
    }

    /** Vision API のリクエスト数が上限に達している場合にスローされる例外 */
    class VisionApiLimitExceededException : Throwable()

    /** Vision API サポート外の MIME タイプが指定された場合にスローされる例外 */
    class VisionApiUnsupportedMimeTypeException(mimeType: String) : Throwable("Unsupported MIME type: $mimeType")

    /** Vision API のエラー例外 */
    class VisionApiErrorException(status: Status) : Throwable(status.message)

    /** Vision API の不明なエラー例外 */
    class VisionApiUnknownErrorException(cause: Throwable) : Throwable(cause)

    /** File の加算拡張関数: `this + file` で `this` と `file` を連結する。 */
    private operator fun File.plus(file: File) = File(this, file.name)

    // Systemがモックできないので、ラップする
    // https://toranoana-lab.hatenablog.com/entry/2023/09/26/100000
    // https://github.com/mockk/mockk/issues/98
    /** Google Application Credentials が存在するか確認する */
    fun isGoogleAppCredentialsExist(): Boolean {
        return File(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")).exists()
    }
}