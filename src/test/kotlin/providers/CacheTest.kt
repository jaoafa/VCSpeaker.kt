package providers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.actions.CacheAction
import com.jaoafa.vcspeaker.database.tables.SpeechCacheEntity
import com.jaoafa.vcspeaker.database.tables.SpeechCacheTable
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL
import java.nio.file.Files
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class CacheActionTest : FunSpec({
    lateinit var tempCacheDir: java.io.File

    beforeSpec {
        DatabaseUtil.connect(TEST_DB_MEM_URL)
        DatabaseUtil.createTables()
    }

    beforeEach {
        tempCacheDir = Files.createTempDirectory("cache-action-test").toFile()
        VCSpeaker.cacheFolder = tempCacheDir
    }

    afterEach {
        transaction {
            SpeechCacheTable.deleteAll()
        }
        tempCacheDir.deleteRecursively()
    }

    fun getHashString(i: Int) = "%032x".format(i)

    test("cleanCache should keep newest 100 entries and delete older cache files") {
        val total = 105

        val format = VoiceTextProvider.format

        transaction {
            repeat(total) { i ->
                val hash = getHashString(i)
                SpeechCacheEntity.new {
                    providerId = VoiceTextProvider.id
                    this.hash = hash
                    lastUsedAt = Clock.System.now() - i.milliseconds
                }

                VCSpeaker.cacheFolder.resolve("$hash.$format").writeText("sample-data-$i")
            }
        }

        val dropped = CacheAction.cleanCache()

        dropped shouldBe 5

        transaction {
            SpeechCacheEntity.all().count() shouldBe 100
        }

        repeat(100) { i ->
            val hash = getHashString(i)
            VCSpeaker.cacheFolder.resolve("$hash.$format").exists() shouldBe true
        }

        repeat(5) { i ->
            val i = 100 + i
            val hash = getHashString(i)
            VCSpeaker.cacheFolder.resolve("$hash.$format").exists() shouldBe false
        }
    }
})
