import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.MarkdownFormatProcessor
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class MarkdownFormatProcessorTest : FunSpec({
    test("Process returns content converted from inline markdown") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(
            message,
            "**bold** *italic* [link](http://example.com)",
            voice
        )

        processedText shouldBe "bold italic link"
        processedVoice shouldBe voice
    }

    test("Process returns content converted from block markdown") {
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

        processedText shouldBe "# Header 1## Header 2### Header 3#### Header 4##### Header 5###### Header 6- List item 1- List item 2- List item 31. Numbered list item 12. Numbered list item 23. Numbered list item 3> BlockquoteCode"
        processedVoice shouldBe voice
    }

    test("Process returns content with no changes when no markdown") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "no markdown here", voice)

        processedText shouldBe "no markdown here"
        processedVoice shouldBe voice
    }

    test("Process returns content with no changes when empty string") {
        val message = mockk<Message>()
        val processor = MarkdownFormatProcessor()
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = processor.process(message, "", voice)

        processedText shouldBe ""
        processedVoice shouldBe voice
    }
})