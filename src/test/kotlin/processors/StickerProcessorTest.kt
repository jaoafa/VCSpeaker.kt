package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
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
    test("If the message has no sticker, the text should be remain unchanged.") {
        val message = mockk<Message>()
        every { message.stickers } returns emptyList()

        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = StickerProcessor().process(message, "original content", voice)

        processedText shouldBe "original content"
        processedVoice shouldBe voice
    }

    // スタンプが存在する場合、スタンプ名を含んだコンテンツを返す
    test("If the message has stickers, the text appended with sticker names should be returned.") {
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
    test("If the message has stickers but the text is blank, only the sticker names should be returned.") {
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