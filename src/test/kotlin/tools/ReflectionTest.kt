package tools

import com.jaoafa.vcspeaker.tools.getClassesIn
import com.jaoafa.vcspeaker.tts.replacers.BaseReplacer
import com.jaoafa.vcspeaker.tts.replacers.UserMentionReplacer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

class ReflectionTest : FunSpec({
    test("getClassesIn should find replacer classes") {
        val classes = getClassesIn<BaseReplacer>("com.jaoafa.vcspeaker.tts.replacers")
        classes shouldContain UserMentionReplacer::class.java
    }
})
