package processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.MarkdownFormatProcessor
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk

/**
 * MarkdownFormatProcessor のテスト
 */
class MarkdownFormatProcessorTest : FunSpec({
    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // インラインの太字マークダウンから変換されたコンテンツを返す
    test("Process returns content converted from inline bold markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "**bold**",
            voice
        )

        processedText shouldBe "bold"
        processedVoice shouldBe voice
    }

    // インラインの斜体マークダウンから変換されたコンテンツを返す
    test("Process returns content converted from inline italic markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "*italic*",
            voice
        )

        processedText shouldBe "italic"
        processedVoice shouldBe voice
    }

    // インラインの取り消し線マークダウンから変換されたコンテンツを返す
    test("Process returns content converted from inline strike-through markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "~~strike-through~~",
            voice
        )

        processedText shouldBe "パー"
        processedVoice shouldBe voice
    }

    // インラインの下線マークダウンから変換されたコンテンツを返す
    test("Process returns content converted from inline underline markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "__underline__",
            voice
        )

        processedText shouldBe "underline"
        processedVoice shouldBe voice
    }

    // インラインのコードマークダウンから変換されたコンテンツを返す
    test("Process returns content converted from inline code markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "`code`",
            voice
        )

        processedText shouldBe "code"
        processedVoice shouldBe voice
    }

    // インラインのリンクマークダウンから変換されたコンテンツを返す
    test("Process returns content converted from inline link markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "[link](https://example.com)",
            voice
        )

        processedText shouldBe "link"
        processedVoice shouldBe voice
    }

    // インラインの引用マークダウンから変換されたコンテンツを返す
    test("Process returns content converted from inline spoiler markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "||spoiler||",
            voice
        )

        processedText shouldBe "ピー"
        processedVoice shouldBe voice
    }

    // ブロックマークダウンは改行とコードブロックを除去して返す
    test("Process returns content converted from block markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message, """
            # Header 1
            ## Header 2
            ### Header 3
            #### Header 4
            ##### Header 5
            ###### Header 6
            - List item 1
            - List item 2
            - List item 3
            1. Numbered list item 1
            2. Numbered list item 2
            3. Numbered list item 3
            > Blockquote
            `Code`
            ```kotlin
            fun main() {
                println("Hello, world!")
            }
            ```
        """.trimIndent(), voice
        )

        // TODO ここは実装に改良の余地がありそう。改行を消すときにスペースくらい残さないと、前の文字とくっついてしまう
        processedText shouldBe "# Header 1## Header 2### Header 3#### Header 4##### Header 5###### Header 6- List item 1- List item 2- List item 31. Numbered list item 12. Numbered list item 23. Numbered list item 3> BlockquoteCode"
        processedVoice shouldBe voice
    }

    // マークダウンがない場合、変更なしのコンテンツを返す
    test("Process returns content with no changes when no markdown.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "no markdown here", voice)

        processedText shouldBe "no markdown here"
        processedVoice shouldBe voice
    }

    // 空の文字列の場合、変更なしのコンテンツを返す
    test("Process returns content with no changes when empty string.") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "", voice)

        processedText shouldBe ""
        processedVoice shouldBe voice
    }
})