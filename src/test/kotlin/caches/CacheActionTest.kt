package caches

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.actions.CacheAction
import com.jaoafa.vcspeaker.database.tables.SpeechCacheEntity
import com.jaoafa.vcspeaker.database.tables.SpeechCacheTable
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL
import java.io.File
import kotlin.time.Clock

class CacheActionTest : FunSpec({
    beforeSpec {
        DatabaseUtil.connect(TEST_DB_MEM_URL)
        DatabaseUtil.createTables()
    }

    beforeEach {
        mockkObject(VCSpeaker)
        every { VCSpeaker.cacheFolder } returns File(
            System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker"
        )

        VCSpeaker.cacheFolder.mkdirs()
    }

    afterEach {
        transaction {
            SpeechCacheTable.deleteAll()
        }
        VCSpeaker.cacheFolder.deleteRecursively()
        clearAllMocks()
    }

    val context1 = SoundmojiContext(id = Snowflake(1L))
    val byteArray1 = byteArrayOf(1, 2, 3)

    test("createOrUpdate creates new cache entry and corresponding object file") {
        CacheAction.createOrUpdate(context1, byteArray1)

        val entry = transaction {
            SpeechCacheEntity.count() shouldBe 1
            SpeechCacheEntity.find {
                SpeechCacheTable.hash eq context1.hash()
            }.singleOrNull()
        }

        entry shouldNotBe null

        val file = context1.getCacheFile()
        file.exists() shouldBe true
        file.readBytes() shouldBe byteArray1
    }

    test("createOrUpdate updates lastUsedAt on duplicate entry, and re-creates object file") {
        CacheAction.createOrUpdate(context1, byteArray1)

        val timestamp = Clock.System.now()

        val old = transaction {
            SpeechCacheEntity.find {
                SpeechCacheTable.hash eq context1.hash()
            }.single().getSnapshot()
        }

        old.lastUsedAt shouldBeLessThan timestamp

        context1.getCacheFile().delete() shouldBe true

        CacheAction.createOrUpdate(context1, byteArray1)

        val new = transaction {
            SpeechCacheEntity.find {
                SpeechCacheTable.hash eq context1.hash()
            }.single().getSnapshot()
        }

        new.lastUsedAt shouldBeGreaterThan timestamp
        context1.getCacheFile().also {
            it.exists() shouldBe true
            it.readBytes() shouldBe byteArray1
        }
    }
})
