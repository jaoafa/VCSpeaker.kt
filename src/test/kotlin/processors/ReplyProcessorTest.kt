package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.ReplyProcessor
import dev.kord.core.entity.Message
import dev.kord.core.entity.effectiveName
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

/**
 * ReplyProcessor のテスト
 */
class ReplyProcessorTest : FunSpec({
    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // メッセージに返信するとき、返信先のユーザー名を読み上げる
    test("If the message is a reply, read the author name of the replied message.") {
        val message = mockk<Message>()
        every { message.referencedMessage } returns mockk {
            every { author?.effectiveName } returns "User"
        }

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = ReplyProcessor().process(message, "test", voice)
        processedText shouldBe "User への返信、test"
        processedVoice shouldBe voice
    }

    // 返信メッセージでない場合、そのまま読み上げる
    test("If the message is not a reply, read it as is.") {
        val message = mockk<Message>()
        every { message.referencedMessage } returns null

        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = ReplyProcessor().process(message, "test", voice)
        processedText shouldBe "test"
        processedVoice shouldBe voice
    }

    // 作者のないメッセージに返信すると、不明な返信として読み上げる
    test("If the message is a reply but the author of the replied message is unknown, read it as a reply to an unknown author.") {
        val message = mockk<Message>()
        every { message.referencedMessage } returns mockk {
            every { author?.globalName } returns null
            every { author?.username } returns null
            every { author?.effectiveName } returns null
        }

        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = ReplyProcessor().process(message, "test", voice)
        processedText shouldBe "だれか への返信、test"
        processedVoice shouldBe voice
    }
})