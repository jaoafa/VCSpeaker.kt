package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.processors.InlineVoiceProcessor
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.core.entity.Message
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
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
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "original content", voice)

        processedText shouldBe "original content"
        processedVoice shouldBe voice
    }

    // インライン音声パラメータがある場合、プロセスはコンテンツと音声パラメータを返す
    test("If inline voice parameters exist, the text and voice parameters should be returned.") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "test speaker:haruka emotion:happiness emotion_level:1 pitch:100 speed:100",
            voice
        )

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
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val exception = shouldThrow<IllegalArgumentException> {
            processor.process(message, "test speaker:invalid", voice)
        }
        exception.message shouldBe "No enum constant com.jaoafa.vcspeaker.tts.api.Speaker.Invalid"
    }

    // 無効な感情名が指定された場合、例外がスローされる
    test("If an invalid emotion name is specified, an exception should be thrown.") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val exception = shouldThrow<IllegalArgumentException> {
            processor.process(message, "test emotion:invalid", voice)
        }
        exception.message shouldBe "No enum constant com.jaoafa.vcspeaker.tts.api.Emotion.Invalid"
    }

    // 無効な感情レベルが指定された場合、例外がスローされず、値が無視される
    test("If an invalid emotion level is specified, the parameter should be ignored without throwing an exception.") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "test emotion_level:invalid", voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // emotionLevel は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    // 無効なピッチが指定された場合、例外がスローされず、値が無視される
    test("If an invalid pitch is specified, the parameter should be ignored without throwing an exception.") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "test pitch:invalid", voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // pitch は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    // 無効なスピードが指定された場合、例外がスローされず、値が無視される
    test("If an invalid speed is specified, the parameter should be ignored without throwing an exception.") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "test speed:invalid", voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // speed は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    // インライン音声パラメータの構文が無効な場合、コンテンツと音声パラメータを返す
    test("If the syntax of the inline voice parameters is invalid, the parameters should be ignored.") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "test invalid:syntax", voice)

        processedText shouldBe "test invalid:syntax"
        processedVoice shouldBe voice
    }
})