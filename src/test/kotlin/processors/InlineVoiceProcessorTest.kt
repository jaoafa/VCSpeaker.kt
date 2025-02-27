package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import com.jaoafa.vcspeaker.tts.processors.InlineVoiceProcessor
import dev.kord.core.entity.Message
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

/**
 * InlineVoiceProcessor のテスト
 */
class InlineVoiceProcessorTest : FunSpec({
    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // インライン音声パラメータがない場合、プロセスは元のコンテンツを返す
    test("If there is no inline voice parameters, the unchanged text should be returned.") {
        val content = "original content"

        val message = mockk<Message>()
        every { message.content } returns content
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, content, voice)

        processedText shouldBe content
        processedVoice shouldBe voice
    }

    // インライン音声パラメータがある場合、プロセスはコンテンツと音声パラメータを返す
    test("If inline voice parameters exist, the text and voice parameters should be returned.") {
        val content = "test speaker:haruka emotion:happiness emotion_level:1 pitch:100 speed:100"

        val message = mockk<Message>()
        every { message.content } returns content
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, content, voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice.copy(
            speaker = Speaker.Haruka,
            emotion = Emotion.Happiness,
            emotionLevel = 1,
            pitch = 100,
            speed = 100
        )
    }

    // 無効なスピーカー名が指定された場合、例外がスローされる
    test("If an invalid speaker name is specified, an exception should be thrown.") {
        val content = "test speaker:invalid"

        val message = mockk<Message>()
        every { message.content } returns content
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val exception = shouldThrow<IllegalArgumentException> {
            processor.process(message, content, voice)
        }
        exception.message shouldBe "No enum constant com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker.Invalid"
    }

    // 無効な感情名が指定された場合、例外がスローされる
    test("If an invalid emotion name is specified, an exception should be thrown.") {
        val content = "test emotion:invalid"

        val message = mockk<Message>()
        every { message.content } returns content
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val exception = shouldThrow<IllegalArgumentException> {
            processor.process(message, content, voice)
        }
        exception.message shouldBe "No enum constant com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion.Invalid"
    }

    // 無効な感情レベルが指定された場合、例外がスローされず、値が無視される
    test("If an invalid emotion level is specified, the parameter should be ignored without throwing an exception.") {
        val content = "test emotion_level:invalid"

        val message = mockk<Message>()
        every { message.content } returns content
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, content, voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // emotionLevel は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    // 無効なピッチが指定された場合、例外がスローされず、値が無視される
    test("If an invalid pitch is specified, the parameter should be ignored without throwing an exception.") {
        val content = "test pitch:invalid"

        val message = mockk<Message>()
        every { message.content } returns content
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, content, voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // pitch は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    // 無効なスピードが指定された場合、例外がスローされず、値が無視される
    test("If an invalid speed is specified, the parameter should be ignored without throwing an exception.") {
        val content = "test speed:invalid"

        val message = mockk<Message>()
        every { message.content } returns content
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, content, voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // speed は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    // インライン音声パラメータの構文が無効な場合、コンテンツと音声パラメータを返す
    test("If the syntax of the inline voice parameters is invalid, the parameters should be ignored.") {
        val content = "test invalid:syntax"

        val message = mockk<Message>()
        every { message.content } returns content
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, content, voice)

        processedText shouldBe "test invalid:syntax"
        processedVoice shouldBe voice
    }
})