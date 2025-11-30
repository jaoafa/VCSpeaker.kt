package providers

import com.jaoafa.vcspeaker.tts.providers.BatchProvider
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import dev.schlaubi.lavakord.audio.Link
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class BatchProviderTest : FunSpec({
    test("start should throw IllegalArgumentException when context has no provider") {
        val dummy = object : ProviderContext {
            override fun describe(): String = "dummy"
            override fun identity(): String = "dummy"
        }
        val linkMock = mockk<Link>()
        val batch = BatchProvider(linkMock, listOf(dummy))
        shouldThrow<IllegalArgumentException> {
            runBlocking {
                batch.start()
            }
        }
    }
})