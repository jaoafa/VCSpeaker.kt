package processors

import com.jaoafa.vcspeaker.tools.VisionApi
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.AttachmentProcessor
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject

/**
 * AttachmentProcessorのテスト
 */
class AttachmentProcessorTest : FunSpec({
    afterTest {
        clearAllMocks()
    }

    // もしファイルが添付されていない場合、テキストは変更されない
    test("If no attachment, the text should remain unchanged.") {
        val message = mockk<Message>()
        every { message.attachments } returns emptySet()
        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = AttachmentProcessor().process(message, "test", voice)
        processedText shouldBe "test"
        processedVoice shouldBe voice
    }

    // 最初の添付ファイルが画像でない場合は、テキストとファイル名を読み上げる
    test("If the first attachment is not an image, the text and filename should be read.") {
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

    // 最初のファイルが画像でなく、複数のファイルが読み込まれた場合、2つ目以降のファイルは詳細が読み上げられない
    test("If multiple attachments are attached and the first attachment is not an image, the second and subsequent attachments should not be read.") {
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

    // 最初の添付ファイルが画像で、GOOGLE_APPLICATION_CREDENTIALS ファイルが存在しない場合、テキストとファイル名を読み上げる
    test("If the first attachment is an image but GOOGLE_APPLICATION_CREDENTIALS file does not exist, the text and filename should be read.") {
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
})