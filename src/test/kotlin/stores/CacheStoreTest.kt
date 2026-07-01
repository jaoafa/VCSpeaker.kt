package stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.CacheStore
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CacheStoreTest : FunSpec({
    beforeSpec {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker")
        every { VCSpeaker.cacheFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker-cache")
        VCSpeaker.storeFolder.mkdirs()
        VCSpeaker.cacheFolder.mkdirs()

        val cacheFile = File(VCSpeaker.storeFolder, "caches.json")
        if (!cacheFile.exists()) cacheFile.writeText("""{"version":1,"list":[]}""")
    }

    afterTest {
        CacheStore.data.clear()
        VCSpeaker.cacheFolder.listFiles()?.forEach { it.delete() }
    }

    test("Calling readOrCreate() concurrently for the same context should call onNoCache only once.") {
        val context = SoundmojiContext(Snowflake(1111111111111111111UL))
        val callCount = AtomicInteger(0)

        val files = runBlocking {
            coroutineScope {
                (1..20).map {
                    async {
                        CacheStore.readOrCreate(
                            context,
                            onNoCache = {
                                callCount.incrementAndGet()
                                "dummy-audio".toByteArray()
                            },
                            onCached = {}
                        )
                    }
                }.map { it.await() }
            }
        }

        callCount.get() shouldBe 1
        files.map { it.path }.distinct() shouldBe listOf(files.first().path)
        CacheStore.data.count { it.hash == context.hash() } shouldBe 1
    }

    test("Calling readOrCreate() concurrently for many distinct contexts should not throw ConcurrentModificationException.") {
        runBlocking {
            coroutineScope {
                (1..50).map { i ->
                    async {
                        CacheStore.readOrCreate(
                            SoundmojiContext(Snowflake(i.toULong())),
                            onNoCache = { "dummy-audio-$i".toByteArray() },
                            onCached = {}
                        )
                    }
                }.forEach { it.await() }
            }
        }

        CacheStore.data.map { it.hash }.distinct().size shouldBe 50
    }

    test("A cache hit should update lastUsed to a newer timestamp.") {
        val context = SoundmojiContext(Snowflake(2222222222222222222UL))

        CacheStore.readOrCreate(context, onNoCache = { "dummy-audio".toByteArray() }, onCached = {})
        val firstLastUsed = CacheStore.data.first { it.hash == context.hash() }.lastUsed

        Thread.sleep(5)

        var cacheHitCalled = false
        CacheStore.readOrCreate(context, onNoCache = { error("should not be called on cache hit") }, onCached = { cacheHitCalled = true })

        cacheHitCalled shouldBe true
        (CacheStore.data.first { it.hash == context.hash() }.lastUsed > firstLastUsed) shouldBe true
    }
})
