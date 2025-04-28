package tools

import com.jaoafa.vcspeaker.tools.VisionApi
import com.jaoafa.vcspeaker.tools.VisionApi.VisionApiUnsupportedMimeTypeException
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow

class VisionApiTest : FunSpec({
    test("getTextAnnotations throws for unsupported MIME type") {
        val bytes = "not an image".toByteArray()
        shouldThrow<VisionApiUnsupportedMimeTypeException> {
            VisionApi.getTextAnnotations(bytes)
        }
    }
})
