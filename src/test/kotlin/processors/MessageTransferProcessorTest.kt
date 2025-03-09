package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.processors.MessageTransferProcessor
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.common.entity.MessageType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.Message
import dev.kord.core.entity.MessageReference
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

/**
 * MessageTransferProcessor のテスト
 */
class MessageTransferProcessorTest : FunSpec({
    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // 引用メッセージが投稿され、そのメッセージ・チャンネル情報が取得可能でスレッドチャンネルであり、50文字以上の内容である場合、50文字までを読み上げる
    test("If a quoted message is posted and its message and channel information can be obtained and it is a thread channel and its content is more than 50 characters, read up to 50 characters.") {
        val message = mockk<Message>()

        every { message.type } returns MessageType.Default
        every { message.flags } returns MessageFlags(MessageFlag.fromShift(14))

        val messageReference = mockk<MessageReference>()
        coEvery {
            messageReference.channel.fetchChannelOrNull()
        } returns mockk<ThreadChannel> {
            coEvery { asChannelOf<ThreadChannel>() } returns this // これがないと asChannelOf が null を返す
            every { type } returns ChannelType.PublicGuildThread
            every { name } returns "thread"
            coEvery { parent.asChannel().name } returns "parent"
        }

        coEvery {
            messageReference.message?.fetchMessageOrNull()
        } returns mockk {
            every { content } returns "a".repeat(51)
        }

        every { message.messageReference } returns messageReference

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MessageTransferProcessor().process(message, "", voice)

        processedText shouldBe "スレッド「parent」のスレッド「thread」で送信したメッセージ、「aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa 以下略」の引用"
        processedVoice shouldBe voice
    }

    // 引用メッセージが投稿され、そのメッセージ・チャンネル情報が取得可能であり、スレッドチャンネルではない通常スレッドであり、50文字以上の内容である場合、50文字までを読み上げる
    test("If a quoted message is posted and its message and channel information can be obtained and it is a normal thread channel and its content is more than 50 characters, read up to 50 characters.") {
        val message = mockk<Message>()

        every { message.type } returns MessageType.Default
        every { message.flags } returns MessageFlags(MessageFlag.fromShift(14))

        val messageReference = mockk<MessageReference>()
        coEvery {
            messageReference.channel.fetchChannelOrNull()
        } returns mockk<TextChannel> {
            every { type } returns ChannelType.GuildText
            every { name } returns "text"
        }

        coEvery {
            messageReference.message?.fetchMessageOrNull()
        } returns mockk {
            every { content } returns "a".repeat(51)
        }

        every { message.messageReference } returns messageReference

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MessageTransferProcessor().process(message, "", voice)

        processedText shouldBe "テキストチャンネル「text」で送信したメッセージ、「aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa 以下略」の引用"
        processedVoice shouldBe voice
    }

    // 引用メッセージが投稿され、そのメッセージ・チャンネル情報が取得可能であり、50文字以下の内容である場合、そのまま読み上げる
    test("If a quoted message is posted and its message and channel information can be obtained and its content is 50 characters or less, read it as is.") {
        val message = mockk<Message>()

        every { message.type } returns MessageType.Default
        every { message.flags } returns MessageFlags(MessageFlag.fromShift(14))

        val messageReference = mockk<MessageReference>()
        coEvery {
            messageReference.channel.fetchChannelOrNull()
        } returns mockk<TextChannel> {
            every { type } returns ChannelType.GuildText
            every { name } returns "text"
        }

        coEvery {
            messageReference.message?.fetchMessageOrNull()
        } returns mockk {
            every { content } returns "a".repeat(50)
        }

        every { message.messageReference } returns messageReference

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MessageTransferProcessor().process(message, "", voice)

        processedText shouldBe "テキストチャンネル「text」で送信したメッセージ、「aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa」の引用"
        processedVoice shouldBe voice
    }

    // 引用メッセージが投稿され、チャンネル情報は取得できるが、メッセージ情報が取得できない場合、そのチャンネルの情報のみを読み上げる
    test("If a quoted message is posted and its channel information can be obtained but its message information cannot be obtained, read only the channel information.") {
        val message = mockk<Message>()

        every { message.type } returns MessageType.Default
        every { message.flags } returns MessageFlags(MessageFlag.fromShift(14))

        val messageReference = mockk<MessageReference>()
        coEvery {
            messageReference.channel.fetchChannelOrNull()
        } returns mockk<TextChannel> {
            every { type } returns ChannelType.GuildText
            every { name } returns "text"
        }

        coEvery {
            messageReference.message?.fetchMessageOrNull()
        } returns null

        every { message.messageReference } returns messageReference

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MessageTransferProcessor().process(message, "", voice)

        processedText shouldBe "テキストチャンネル「text」で送信したメッセージの引用"
        processedVoice shouldBe voice
    }

    // 引用メッセージが投稿され、チャンネル情報が取得できない場合、その旨を読み上げる
    test("If a quoted message is posted and its channel information cannot be obtained, read that fact.") {
        val message = mockk<Message>()

        every { message.type } returns MessageType.Default
        every { message.flags } returns MessageFlags(MessageFlag.fromShift(14))

        val messageReference = mockk<MessageReference>()
        coEvery {
            messageReference.channel.fetchChannelOrNull()
        } returns null

        every { message.messageReference } returns messageReference

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MessageTransferProcessor().process(message, "", voice)

        processedText shouldBe "どこかのチャンネルで送信したメッセージの引用"
        processedVoice shouldBe voice
    }

    // 引用メッセージではない場合、そのまま読み上げる
    test("If it is not a quoted message, read it as is.") {
        val message = mockk<Message>()
        every { message.type } returns MessageType.Default
        every { message.flags } returns null

        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MessageTransferProcessor().process(message, "", voice)
        processedText shouldBe ""
        processedVoice shouldBe voice
    }
})