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

    context("Make Markdown readable.") {
        // インラインの太字マークダウンから変換されたコンテンツを返す
        test("If the markdown message contains bold phrases, the affixes should be removed.") {
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
        test("If the markdown message contains italic phrases, the affixes should be removed.") {
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
        test("If the markdown message contains strike-through phrases, they should be redacted.") {
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
        test("If the markdown message contains underlined phrases, the affixes should be removed.") {
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
        test("If the markdown message contains inline codes, the affixes should be removed.") {
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
        test("If the markdown message contains hyperlinks, the affixes and URLs should be removed.") {
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
        test("If the markdown message contains spoilers, they should be redacted.") {
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
        test("If the markdown message contains code blocks, they should be removed.") {
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
                    -# Small header
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

            processedText shouldBe "Header 1 Header 2 Header 3 #### Header 4 ##### Header 5 ###### Header 6 Small header List item 1 List item 2 List item 3 Numbered list item 1 Numbered list item 2 Numbered list item 3 Blockquote Code"
            processedVoice shouldBe voice
        }

        // マークダウンがない場合、変更なしのコンテンツを返す
        test("If the message contains no markdown syntax, the text should be remain unchanged.") {
            val message = mockk<Message>()
            val processor = MarkdownFormatProcessor()
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = processor.process(message, "no markdown here", voice)

            processedText shouldBe "no markdown here"
            processedVoice shouldBe voice
        }

        // 空の文字列の場合、変更なしのコンテンツを返す
        test("If the message is empty, the text should be remain unchanged.") {
            val message = mockk<Message>()
            val processor = MarkdownFormatProcessor()
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = processor.process(message, "", voice)

            processedText shouldBe ""
            processedVoice shouldBe voice
        }
    }

    context("Change reading speed by header level.") {
        // レベル1のヘッダー行のみの場合、速度が変更されること
        test("If the markdown message contains only a level 1 header line, the speed should be changed.") {
            val message = mockk<Message>()
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = MarkdownFormatProcessor().process(
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

            val (processedText, processedVoice) = MarkdownFormatProcessor().process(
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

            val (processedText, processedVoice) = MarkdownFormatProcessor().process(
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

            val (processedText, processedVoice) = MarkdownFormatProcessor().process(
                message,
                "#### header",
                voice
            )

            processedText shouldBe "#### header"
            processedVoice shouldBe voice
        }

        // ヘッダー行では無い通常テキスト場合、速度が変更されないこと
        test("If the markdown message does not contain a header line, the speed should not be changed.") {
            val message = mockk<Message>()
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = MarkdownFormatProcessor().process(
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

            val (processedText, processedVoice) = MarkdownFormatProcessor().process(
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

            val (processedText, processedVoice) = MarkdownFormatProcessor().process(
                message,
                """
            # header
            not header
            """.trimIndent(),
                voice
            )

            processedText shouldBe "header not header"
            processedVoice shouldBe voice
        }

        // ヘッダー行が複数ある場合、速度が変更されないこと
        test("If there are multiple header lines, the speed should not be changed.") {
            val message = mockk<Message>()
            val voice = Voice(speaker = Speaker.Hikari)

            val (processedText, processedVoice) = MarkdownFormatProcessor().process(
                message,
                """
            # header1
            # header2
            """.trimIndent(),
                voice
            )

            processedText shouldBe "header1 header2"
            processedVoice shouldBe voice
        }
    }
})