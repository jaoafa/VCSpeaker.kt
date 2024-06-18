import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Emotion
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.InlineVoiceProcessor
import dev.kord.core.entity.Message
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class InlineVoiceProcessorTest : FunSpec({
    test("Process returns original content when no inline voice parameters") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "original content", voice)

        processedText shouldBe "original content"
        processedVoice shouldBe voice
    }

    test("Process returns content with inline voice parameters") {
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

    // 異常値
    test("Process returns content with inline voice parameters with invalid speaker name") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val exception = shouldThrow<IllegalArgumentException> {
            processor.process(message, "test speaker:invalid", voice)
        }
        exception.message shouldBe "No enum constant com.jaoafa.vcspeaker.tts.api.Speaker.Invalid"
    }

    test("Process returns content with inline voice parameters with invalid emotion name") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val exception = shouldThrow<IllegalArgumentException> {
            processor.process(message, "test emotion:invalid", voice)
        }
        exception.message shouldBe "No enum constant com.jaoafa.vcspeaker.tts.api.Emotion.Invalid"
    }

    test("Process returns content with inline voice parameters with invalid emotion level") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "test emotion_level:invalid", voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // emotionLevel は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    test("Process returns content with inline voice parameters with invalid pitch") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "test pitch:invalid", voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // pitch は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    test("Process returns content with inline voice parameters with invalid speed") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "test speed:invalid", voice)

        processedText shouldBe "test"
        processedVoice shouldBe voice // speed は変更されない。ユーザーが入力ミスをした場合は無視される形になるので、良いか悪いか…
    }

    test("Process returns content with inline voice parameters with invalid syntax") {
        val message = mockk<Message>()
        val processor = InlineVoiceProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "test invalid:syntax", voice)

        processedText shouldBe "test invalid:syntax"
        processedVoice shouldBe voice
    }
})