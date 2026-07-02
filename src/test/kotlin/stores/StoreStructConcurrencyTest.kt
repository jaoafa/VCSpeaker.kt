package stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.StoreStruct
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class DummyData(val value: Int)

class StoreStructConcurrencyTest : FunSpec({
    lateinit var store: StoreStruct<DummyData>

    beforeSpec {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker")
        VCSpeaker.storeFolder.mkdirs()
    }

    beforeTest {
        val file = File(VCSpeaker.storeFolder, "storestruct-concurrency-test.json")
        file.delete()
        store = StoreStruct(
            file.path,
            DummyData.serializer(),
            { Json.decodeFromString(this) }
        )
    }

    test("Calling create() from many coroutines concurrently should not throw ConcurrentModificationException and should keep all elements.") {
        runBlocking {
            coroutineScope {
                val jobs = (1..200).map { i ->
                    async { store.create(DummyData(i)) }
                }
                jobs.forEach { it.await() }
            }
        }

        store.data.map { it.value }.sorted() shouldBe (1..200).toList()
    }

    test("Interleaving create() and remove() from many coroutines concurrently should not throw ConcurrentModificationException.") {
        runBlocking {
            coroutineScope {
                (1..100).forEach { store.create(DummyData(it)) }

                val jobs = (1..100).map { i ->
                    async {
                        if (i % 2 == 0) store.remove(DummyData(i)) else store.create(DummyData(i + 1000))
                    }
                }
                jobs.forEach { it.await() }
            }
        }

        // no exception thrown is the primary assertion; sanity-check parity of survivors
        store.data.none { it.value in 2..100 step 2 } shouldBe true
    }
})
