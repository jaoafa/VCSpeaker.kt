package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.PinMessageProcessor
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import dev.kord.core.entity.MessageReference
import dev.kord.core.entity.effectiveName
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

/**
 * PinMessageProcessor のテスト
 */
class PinMessageProcessorTest : FunSpec({
    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // メッセージにピン止めするとき、ピン止めした旨を読み上げる
    test("If the message is a pinned notify message, read that the message is pinned.") {
        val message = mockk<Message>()

        every { message.type } returns MessageType.ChannelPinnedMessage

        val messageReference = mockk<MessageReference>()
        every { messageReference.message?.id } returns Snowflake(123)

        every { message.author?.effectiveName } returns "User"
        every { message.messageReference } returns messageReference

        val pinnedMessage = mockk<Message>()
        every { pinnedMessage.author?.effectiveName } returns "PinnedUser"
        coEvery { message.channel.getMessage(Snowflake(123)) } returns pinnedMessage

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = PinMessageProcessor().process(message, "", voice)
        processedText shouldBe "User が PinnedUser のメッセージをピン止めしました"
        processedVoice shouldBe voice
    }

    // 返信メッセージでない場合、そのまま読み上げる
    test("If the message is not a pinned notify message, read it as is.") {
        val message = mockk<Message>()
        every { message.type } returns MessageType.Default
        every { message.referencedMessage } returns null

        val voice = Voice(speaker = Speaker.Hikari)
        val (processedText, processedVoice) = PinMessageProcessor().process(message, "", voice)
        processedText shouldBe ""
        processedVoice shouldBe voice
    }

    // 作者のないメッセージをピン止めすると、不明なメッセージをピン止めしたものとして読み上げる
    test("If the message is a pinned notify message but the author of the pinned message is unknown, read it as a pinned to an unknown author.") {
        val message = mockk<Message>()

        every { message.type } returns MessageType.ChannelPinnedMessage

        val messageReference = mockk<MessageReference>()
        every { messageReference.message?.id } returns Snowflake(123)

        every { message.author?.effectiveName } returns "User"
        every { message.messageReference } returns messageReference

        val pinnedMessage = mockk<Message>()
        every { pinnedMessage.author } returns null
        coEvery { message.channel.getMessage(Snowflake(123)) } returns pinnedMessage

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = PinMessageProcessor().process(message, "", voice)
        processedText shouldBe "User が だれか のメッセージをピン止めしました"
        processedVoice shouldBe voice
    }
})