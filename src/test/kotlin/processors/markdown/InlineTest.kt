package processors.markdown

import com.jaoafa.vcspeaker.tts.markdown.Inline
import com.jaoafa.vcspeaker.tts.markdown.InlineEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InlineTest : FunSpec({
    test("Italic-underline overlap") {
        val inlines = Inline.from("___italic underline___")
        inlines shouldBe listOf(
            Inline("italic underline", mutableSetOf(InlineEffect.Italic, InlineEffect.Underline))
        )
    }

    test("Italic-bold overlap") {
        val inlines = Inline.from("***italic bold***")
        inlines shouldBe listOf(
            Inline("italic bold", mutableSetOf(InlineEffect.Italic, InlineEffect.Bold))
        )
    }

    test("Italic-underline partial overlap") {
        val inlines = Inline.from("___italic_ underline__")
        inlines shouldBe listOf(
            Inline("italic", mutableSetOf(InlineEffect.Italic, InlineEffect.Underline)),
            Inline(" underline", mutableSetOf(InlineEffect.Underline))
        )
    }

    test("Italic-bold partial overlap") {
        val inlines = Inline.from("***italic* bold**")
        inlines shouldBe listOf(
            Inline("italic", mutableSetOf(InlineEffect.Italic, InlineEffect.Bold)),
            Inline(" bold", mutableSetOf(InlineEffect.Bold))
        )
    }

    test("No overlap") {
        val inlines = Inline.from("**bold** ||spoiler||")
        inlines shouldBe listOf(
            Inline("bold", mutableSetOf(InlineEffect.Bold)),
            Inline(" ", mutableSetOf()),
            Inline("spoiler", mutableSetOf(InlineEffect.Spoiler))
        )
    }
})