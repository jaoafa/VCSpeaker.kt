package stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GameStore
import com.jaoafa.vcspeaker.tools.discord.GameResolver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import java.io.File

class GameStoreTest : FunSpec({
    beforeSpec {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker-test-${System.currentTimeMillis()}")
        VCSpeaker.storeFolder.mkdirs()

        val gameFile = File(VCSpeaker.storeFolder, "games.json")
        gameFile.writeText("""{"version":1,"list":[]}""")
    }

    afterTest {
        GameStore.data.clear()
    }

    // --- replaceAll / find ---

    context("replaceAll") {
        test("If replaceAll is called, find should return the stored name for each id.") {
            GameStore.replaceAll(mapOf(1L to "Game One", 2L to "Game Two"), 1000L)

            GameStore.find(1L)?.name shouldBe "Game One"
            GameStore.find(2L)?.name shouldBe "Game Two"
        }

        test("If replaceAll is called, an id not in the map should not be found.") {
            GameStore.replaceAll(mapOf(1L to "Game One"), 1000L)

            GameStore.find(999L).shouldBeNull()
        }

        test("If replaceAll is called again with a different map, previous entries not in the new map should be removed.") {
            GameStore.replaceAll(mapOf(1L to "Game One"), 1000L)
            GameStore.replaceAll(mapOf(2L to "Game Two"), 2000L)

            GameStore.find(1L).shouldBeNull()
            GameStore.find(2L)?.name shouldBe "Game Two"
        }

        test("If replaceAll is called, every entry should be stamped with the given updatedAt.") {
            GameStore.replaceAll(mapOf(1L to "Game One", 2L to "Game Two"), 1234L)

            GameStore.find(1L)?.updatedAt shouldBe 1234L
            GameStore.find(2L)?.updatedAt shouldBe 1234L
        }
    }

    // --- findAll ---

    context("findAll") {
        test("If findAll is called, it should return only the requested ids that exist.") {
            GameStore.replaceAll(mapOf(1L to "Game One", 2L to "Game Two"), 1000L)

            GameStore.findAll(setOf(1L, 999L)) shouldBe mapOf(1L to "Game One")
        }
    }

    // --- lastFetchedAt ---

    context("lastFetchedAt") {
        test("If no entry is stored, lastFetchedAt should return null.") {
            GameResolver.lastFetchedAt.shouldBeNull()
        }

        test("If entries are stored, lastFetchedAt should return the given updatedAt.") {
            GameStore.replaceAll(mapOf(1L to "Game One", 2L to "Game Two"), 5000L)

            GameStore.lastFetchedAt() shouldBe 5000L
        }

        test("If replaceAll is called with an empty map, lastFetchedAt should still return the given updatedAt.") {
            GameStore.replaceAll(emptyMap(), 6000L)

            GameStore.lastFetchedAt() shouldBe 6000L
        }
    }
})
