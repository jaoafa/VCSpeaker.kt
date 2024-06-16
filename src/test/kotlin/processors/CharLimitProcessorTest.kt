package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.CharLimitProcessor
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class CharLimitProcessorTest : FunSpec({
    test("If the text is less than 180 characters, the text remains unchanged") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = CharLimitProcessor().process(message, "test", voice)
        processedText shouldBe "test"
        processedVoice shouldBe voice
    }

    test("If the text is 180 characters, the text remains unchanged") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = CharLimitProcessor().process(message, "a".repeat(180), voice)
        processedText shouldBe "a".repeat(180)
        processedVoice shouldBe voice
    }

    test("If the text is 180 characters or more, the text is truncated to 180 characters") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = CharLimitProcessor().process(message, "a".repeat(181), voice)
        processedText shouldBe "a".repeat(180)
        processedVoice shouldBe voice
    }
})