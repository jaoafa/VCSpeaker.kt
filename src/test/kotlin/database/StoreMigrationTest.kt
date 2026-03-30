package database

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.StoreDBMigrator
import io.kotest.core.spec.style.FunSpec
import utils.Constants.TEST_DB_FILE_URL
import java.io.File

class StoreMigrationTest : FunSpec({
    test("Migration Test") {
        VCSpeaker.storeFolder = File("test-data/store")
        VCSpeaker.cacheFolder = File("test-data/cache")
        StoreDBMigrator.run(TEST_DB_FILE_URL)
    }
})
