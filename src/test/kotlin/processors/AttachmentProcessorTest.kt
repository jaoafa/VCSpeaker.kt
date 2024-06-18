package processors

import com.jaoafa.vcspeaker.tools.VisionApi
import com.jaoafa.vcspeaker.tools.VisionTextAnnotation
import com.jaoafa.vcspeaker.tools.VisionVertex
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.AttachmentProcessor
import com.kotlindiscord.kord.extensions.utils.download
import com.sksamuel.scrimage.ImmutableImage
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class AttachmentProcessorTest : FunSpec({
    test("If no file is attached, the text remains unchanged") {
        val message = mockk<Message>()
        every { message.attachments } returns emptySet()
        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = AttachmentProcessor().process(message, "test", voice)
        processedText shouldBe "test"
        processedVoice shouldBe voice
    }

    test("If the first attachment is not an image, read the text and filename") {
        // 画像ではないファイル test.txt
        val attachment = mockk<Attachment>()
        every { attachment.isImage } returns false
        every { attachment.filename } returns "test.txt"

        val message = mockk<Message>()
        every { message.attachments } returns setOf(attachment)

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = AttachmentProcessor().process(message, "test", voice)

        processedText shouldBe "test 添付ファイル test.txt "
        processedVoice shouldBe voice
    }

    test("If the first file is not an image and multiple files are read, the second and subsequent files are not detailed") {
        val attachment1 = mockk<Attachment>()
        every { attachment1.isImage } returns false
        every { attachment1.filename } returns "test1.txt"

        val attachment2 = mockk<Attachment>()
        every { attachment2.isImage } returns false
        every { attachment2.filename } returns "test2.txt"

        val message = mockk<Message>()
        every { message.attachments } returns setOf(attachment1, attachment2)

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = AttachmentProcessor().process(message, "test", voice)

        processedText shouldBe "test 添付ファイル test1.txt と 1個のファイル"
        processedVoice shouldBe voice
    }

    test("If the first attachment is an image and the GOOGLE_APPLICATION_CREDENTIALS file does not exist, read the text and filename") {
        val attachment = mockk<Attachment>()
        every { attachment.isImage } returns true
        every { attachment.isSpoiler } returns false
        every { attachment.filename } returns "image.png"

        val message = mockk<Message>()
        every { message.attachments } returns setOf(attachment)

        mockkObject(VisionApi)
        every { VisionApi.isGoogleAppCredentialsExist() } returns false

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = AttachmentProcessor().process(message, "test", voice)

        processedText shouldBe "test 画像ファイル image.png "
        processedVoice shouldBe voice
    }

    test("download") {
        // 画像ファイル image.png
        mockkStatic("com.kotlindiscord.kord.extensions.utils._AttachmentsKt")
        val attachment = mockk<Attachment>()
        every { attachment.isImage } returns true
        every { attachment.isSpoiler } returns false
        every { attachment.filename } returns "image.png"
        coEvery { attachment.download() } returns byteArrayOf(0x00)

        // VisionApi
        mockkObject(VisionApi)
        every { VisionApi.isGoogleAppCredentialsExist() } returns true
        every { VisionApi.getTextAnnotations(any()) } returns listOf(
            VisionTextAnnotation(
                "test",
                "ja",
                1.0f,
                listOf(VisionVertex(0, 0), VisionVertex(0, 0), VisionVertex(0, 0), VisionVertex(0, 0))
            )
        )
        every { VisionApi.drawTextAnnotations(any()) } returns mockk<ImmutableImage>()

        // io.mockk.MockKException: no answer found for ImmutableImage(#43).output(com.sksamuel.scrimage.nio.PngWriter@651cf0a2, C:\Users\tomachi\AppData\Local\Temp\Image_1248740629635088695.png) among the configured answers: ()

        val message = mockk<Message>()
        every { message.attachments } returns setOf(attachment)

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = AttachmentProcessor().process(message, "test", voice)

        processedText shouldBe "test 画像ファイル image.png "
        processedVoice shouldBe voice
    }
})