package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.MarkdownHeadingProcessor
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk

/**
 * MarkdownHeadingProcessor のテスト
 */
class MarkdownHeadingProcessorTest : FunSpec({
    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // レベル1のヘッダー行のみの場合、速度が変更されること
    test("If the markdown message contains only a level 1 header line, the speed should be changed.") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MarkdownHeadingProcessor().process(
            message,
            "# header",
            voice
        )

        processedText shouldBe "header"
        processedVoice shouldBe voice.copy(speed = 200)
    }

    // レベル2のヘッダー行のみの場合、速度が変更されること
    test("If the markdown message contains only a level 2 header line, the speed should be changed.") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MarkdownHeadingProcessor().process(
            message,
            "## header",
            voice
        )

        processedText shouldBe "header"
        processedVoice shouldBe voice.copy(speed = 175)
    }

    // レベル3のヘッダー行のみの場合、速度が変更されること
    test("If the markdown message contains only a level 3 header line, the speed should be changed.") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MarkdownHeadingProcessor().process(
            message,
            "### header",
            voice
        )

        processedText shouldBe "header"
        processedVoice shouldBe voice.copy(speed = 150)
    }

    // レベル4のヘッダー行のみの場合、速度が変更されないこと
    test("If the markdown message contains only a level 4 header line, the speed should not be changed.") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MarkdownHeadingProcessor().process(
            message,
            "#### header",
            voice
        )

        processedText shouldBe "header"
        processedVoice shouldBe voice
    }

    // ヘッダー行では無い通常テキスト場合、速度が変更されないこと
    test("If the markdown message does not contain a header line, the speed should not be changed.") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MarkdownHeadingProcessor().process(
            message,
            "not header",
            voice
        )

        processedText shouldBe "not header"
        processedVoice shouldBe voice
    }

    // ヘッダー行内に他の効果がある場合、速度が変更されるが、フォーマットは削除されること
    test("If the markdown message contains other effects in the header line, the speed should be changed, but the format should be removed.") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MarkdownHeadingProcessor().process(
            message,
            "# **header**",
            voice
        )

        processedText shouldBe "header"
        processedVoice shouldBe voice.copy(speed = 200)
    }

    // ヘッダー行以外の行がある場合、速度が変更されないこと
    test("If there are lines other than the header line, the speed should not be changed.") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MarkdownHeadingProcessor().process(
            message,
            """
            # header
            not header
            """.trimIndent(),
            voice
        )

        processedText shouldBe """
            # header
            not header
        """.trimIndent()
        processedVoice shouldBe voice
    }

    // ヘッダー行が複数ある場合、速度が変更されないこと
    test("If there are multiple header lines, the speed should not be changed.") {
        val message = mockk<Message>()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = MarkdownHeadingProcessor().process(
            message,
            """
            # header1
            # header2
            """.trimIndent(),
            voice
        )

        processedText shouldBe """
            # header1
            # header2
        """.trimIndent()
        processedVoice shouldBe voice
    }
})