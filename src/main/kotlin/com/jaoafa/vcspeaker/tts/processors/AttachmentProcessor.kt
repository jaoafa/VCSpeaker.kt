package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.StringUtils.substringByCodePoints
import com.jaoafa.vcspeaker.stores.VisionApiCounterStore
import com.jaoafa.vcspeaker.tools.VisionApi
import com.jaoafa.vcspeaker.tts.Voice
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.addFile
import dev.kordex.core.utils.download
import java.io.File
import java.nio.file.Path

class AttachmentProcessor : BaseProcessor() {
    override val priority = 40

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val attachments = message?.attachments ?: return content to voice
        if (attachments.isEmpty()) return content to voice

        val fileText = getReadFileText(message) ?: return content to voice
        return if (content.isBlank()) fileText to voice else "$content $fileText" to voice
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

        if (!VisionApi.isGoogleAppCredentialsExist()) {
            return "画像ファイル ${firstAttachment.filename} $moreFileRead"
        }

        // 画像解析を行う
        val isSpoiler = firstAttachment.isSpoiler
        val binaryArray = firstAttachment.download()
        try {
            val textAnnotations = VisionApi.getTextAnnotations(binaryArray)
            // 改行は半角スペースに置換する
            val firstDescription = textAnnotations.firstOrNull()?.description?.replace("\n", " ") ?: ""
            val shortDescription =
                if (firstDescription.length > 20) firstDescription.substringByCodePoints(0, 20) + "..." else firstDescription
            val embedDescription =
                if (firstDescription.length > 1000) firstDescription.substringByCodePoints(0, 1000) + "..." else firstDescription

            // 画像解析結果を返信する
            val editedImage = VisionApi.drawTextAnnotations(binaryArray)
            val filePath = editedImage.outputTempFile(isSpoiler)
            val visionApiCounterStore = VisionApiCounterStore.get()

            val requestedCount = visionApiCounterStore?.count ?: 0
            val requestLimit = VisionApiCounterStore.VISION_API_LIMIT
            val remainingRequests = requestLimit - requestedCount

            val spoilerPrefixSuffix = if (isSpoiler) "||" else ""
            message.reply {
                embeds = mutableListOf(
                    EmbedBuilder().apply {
                        description = "$spoilerPrefixSuffix```$embedDescription```$spoilerPrefixSuffix"
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

            return if (isSpoiler) {
                // スポイラーファイルの場合は、スポイラー画像ファイルとして読み上げ
                "スポイラー画像ファイル $moreFileRead"
            } else {
                "$shortDescription を含む画像ファイル $moreFileRead"
            }
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

    private fun ImmutableImage.outputTempFile(isSpoiler: Boolean): Path {
        val prefix = if (isSpoiler) "SPOILER_" else "Image_"
        val tempFile = File.createTempFile(prefix, ".png")
        this.output(PngWriter(), tempFile)
        return tempFile.toPath()
    }
}