package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.stores.VisionApiCounterStore
import com.jaoafa.vcspeaker.tools.VisionApi
import com.kotlindiscord.kord.extensions.utils.download
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.addFile
import java.io.File
import java.nio.file.Path

object MessageProcessor {
    suspend fun processMessage(message: Message?): String? {
        if (message == null) return null

        val stickers = message.stickers
        val attachments = message.attachments
        val content = message.content

        if (stickers.isNotEmpty())
            return stickers.joinToString(" ") { "スタンプ ${it.name}" }

        if (attachments.isNotEmpty()) {
            val fileText = getReadFileText(message)

            return if (content.isBlank()) fileText else "$content $fileText"
        }

        return content.ifBlank { null }
    }

    /**
     * ファイルについての読み上げ文章を作成する。
     *
     * ・画像の場合は画像解析を行い、テキストを取得する。「<テキスト> を含む画像ファイル」という形式で返す。この際、文字起こしした結果を返信する
     * ・画像の場合で解析対象でないなどの場合、「画像ファイル <ファイル名>」という形式で返す。
     * ・それ以外のファイルの場合、「添付ファイル <ファイル名>」という形式で返す。
     */
    private suspend fun getReadFileText(message: Message): String? {
        if (message.attachments.isEmpty()) return null

        // 2つ以上のファイルが添付されている場合、2つ目以降は「と 〇〇個のファイル」という形式で返す
        val moreFileRead = if (message.attachments.size > 1) "と ${message.attachments.size - 1}個のファイル" else ""

        // 1つ目のファイルが画像でなければ、画像解析を行わない
        val firstAttachment = message.attachments.first()
        if (!firstAttachment.isImage) {
            return "添付ファイル ${firstAttachment.filename} $moreFileRead"
        }

        // 画像解析を行う
        val binaryArray = firstAttachment.download()
        try {
            val visionApi = VisionApi()
            val textAnnotations = visionApi.getTextAnnotations(binaryArray)
            // 改行は半角スペースに置換する
            val firstDescription = textAnnotations.firstOrNull()?.description?.replace("\n", " ") ?: ""
            val shortDescription =
                if (firstDescription.length > 20) firstDescription.substring(0, 20) + "..." else firstDescription
            val embedDescription =
                if (firstDescription.length > 1000) firstDescription.substring(0, 1000) + "..." else firstDescription

            // 画像解析結果を返信する
            val editedImage = visionApi.drawTextAnnotations(binaryArray)
            val filePath = editedImage.outputTempFile()
            val visionApiCounterStore = VisionApiCounterStore.get()

            val requestedCount = visionApiCounterStore?.count ?: 0
            val requestLimit = VisionApiCounterStore.VISION_API_LIMIT
            val remainingRequests = requestLimit - requestedCount

            message.reply {
                embeds = mutableListOf(
                    EmbedBuilder().apply {
                        description = "```$embedDescription```"
                        thumbnail = EmbedBuilder.Thumbnail().apply {
                            url = "attachment://${filePath.fileName}"
                        }
                        footer {
                            text = "リクエスト残り回数: $remainingRequests / $requestLimit"
                        }
                    }
                )
                addFile(filePath)
            }

            return "$shortDescription を含む画像ファイル $moreFileRead"
        } catch (_: VisionApi.VisionApiLimitExceededException) {
            // 月のリクエスト数が上限に達している場合、ファイル名のみを読み上げる
            return "画像ファイル ${firstAttachment.filename} $moreFileRead"
        } catch (_: VisionApi.VisionApiUnsupportedMimeTypeException) {
            // サポートされていない MIME タイプの ByteArray が指定された場合、ファイル名のみを読み上げる
            return "画像ファイル ${firstAttachment.filename} $moreFileRead"
        } catch (error: VisionApi.VisionApiErrorException) {
            // エラーを通知する
            message.reply {
                embeds = mutableListOf(
                    EmbedBuilder().apply {
                        title = ":x: 画像解析エラー"
                        description = "画像解析中にエラーが発生しました。"
                        field {
                            name = "エラー内容"
                            value = error.message.toString()
                        }
                    }
                )
            }

            return "画像ファイル ${firstAttachment.filename} $moreFileRead"
        } catch (error: VisionApi.VisionApiUnknownErrorException) {
            // エラーを通知する
            message.reply {
                embeds = mutableListOf(
                    EmbedBuilder().apply {
                        title = ":x: 画像解析エラー"
                        description = "画像解析中に不明なエラーが発生しました。"
                        field {
                            name = "エラー内容"
                            value = error.message.toString()
                        }
                    }
                )
            }

            return "画像ファイル ${firstAttachment.filename} $moreFileRead"
        }
    }

    private fun ImmutableImage.outputTempFile(): Path {
        val tempFile = File.createTempFile("image", ".png")
        this.output(PngWriter(), tempFile)
        return tempFile.toPath()
    }
}