package processors

import com.jaoafa.vcspeaker.tools.VisionApi
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.AttachmentProcessor
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject

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

    // attachment.download() がうまくモックできず (Connection refused: no further information エラー)、テストが作れない…。
})