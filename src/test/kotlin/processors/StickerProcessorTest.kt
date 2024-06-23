package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.StickerProcessor
import dev.kord.core.entity.Message
import dev.kord.core.entity.StickerItem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

/**
 * StickerProcessor のテスト
 */
class StickerProcessorTest : FunSpec({
    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // スタンプがない場合、元のコンテンツを返す
    test("Process returns original content when no stickers") {
        val message = mockk<Message>()
        every { message.stickers } returns emptyList()

        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = StickerProcessor().process(message, "original content", voice)

        processedText shouldBe "original content"
        processedVoice shouldBe voice
    }

    // スタンプが存在する場合、スタンプ名を含んだコンテンツを返す
    test("Process returns content with sticker names when stickers exist") {
        val message = mockk<Message>()
        val sticker = mockk<StickerItem>()
        every { sticker.name } returns "sticker1"
        every { message.stickers } returns listOf(sticker)

        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = StickerProcessor().process(message, "original content", voice)

        processedText shouldBe "original content スタンプ sticker1"
        processedVoice shouldBe voice
    }

    // コンテンツが空白でスタンプが存在する場合、スタンプ名を返す
    test("Process returns sticker names when content is blank and stickers exist") {
        val message = mockk<Message>()
        val sticker = mockk<StickerItem>()
        every { sticker.name } returns "sticker1"
        every { message.stickers } returns listOf(sticker)

        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = StickerProcessor().process(message, "", voice)

        processedText shouldBe "スタンプ sticker1"
        processedVoice shouldBe voice
    }
})