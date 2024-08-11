package processors.markdown

import com.jaoafa.vcspeaker.tts.markdown.Inline
import com.jaoafa.vcspeaker.tts.markdown.Line
import com.jaoafa.vcspeaker.tts.markdown.LineEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LineTest: FunSpec({
    test("Headers") {
        val h1 = Line.from("# Header 1")
        val h2 = Line.from("## Header 2")
        val h3 = Line.from("### Header 3")
        val h4 = Line.from("#### Header 4")
        val h5 = Line.from("##### Header 5")

        h1 shouldBe Line(
            Inline.from("Header 1"),
            setOf(LineEffect.Heading1)
        )

        h2 shouldBe Line(
            Inline.from("Header 2"),
            setOf(LineEffect.Heading2)
        )

        h3 shouldBe Line(
            Inline.from("Header 3"),
            setOf(LineEffect.Heading3)
        )

        h4 shouldBe Line(Inline.from("#### Header 4"), setOf())

        h5 shouldBe Line(Inline.from("##### Header 5"), setOf())
    }

    test("Small heading") {
        val smallHeading = Line.from("-# Small Heading")

        smallHeading shouldBe Line(
            Inline.from("Small Heading"),
            setOf(LineEffect.SmallHeading)
        )
    }

    test("Quote") {
        val quote = Line.from("> Quote")

        quote shouldBe Line(
            Inline.from("Quote"),
            setOf(LineEffect.Quote)
        )
    }

    test("Bullet list") {
        val bulletList = Line.from("* Bullet List")
        val bulletListAlt = Line.from("- Bullet List")

        bulletList shouldBe Line(
            Inline.from("Bullet List"),
            setOf(LineEffect.BulletList)
        )

        bulletListAlt shouldBe Line(
            Inline.from("Bullet List"),
            setOf(LineEffect.BulletList)
        )
    }

    test("Numbered list") {
        for (i in 1..99) {
            val numberedList = Line.from("$i. Numbered List")

            numberedList shouldBe Line(
                Inline.from("Numbered List"),
                setOf(LineEffect.NumberedList)
            )
        }
    }
})