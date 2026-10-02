package utils

import com.jaoafa.vcspeaker.database.DatabaseUtil
import io.kotest.core.spec.AbstractSpec
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL

/**
 * Registers hooks to use database tables in tests.
 * - before spec, connects to DB and creates specified tables.
 * - after each, cleans all entries in all tables, if cleanAfterEach is set to true.
 * - after spec, drops all tables.
 *
 * テスト中に指定されたテーブルを使用するための Lifecycle hooks を登録します。
 * - before spec ... DB に接続し、テーブルを作成
 * - after each ... cleanAfterEach が true の場合、全てのテーブルの全てのエントリを削除
 * - after spec ... 全てのテーブルをドロップ
 *
 * Example:
 * ```
 * class KVStoreTest : FunSpec({
 *     useTables(KVTable)
 *     // ...rest of the test...
 * })
 * ```
 *
 * @param tables The tables to use.
 * @param cleanAfterEach Whether to clean all entries in the tables. Defaults to true.
 */
fun AbstractSpec.useTables(vararg tables: Table, cleanAfterEach: Boolean = true) {
    beforeSpec {
        DatabaseUtil.connect(TEST_DB_MEM_URL)
        DatabaseUtil.createTables(*tables)
    }

    afterEach {
        if (cleanAfterEach) {
            transaction {
                exec("SET REFERENTIAL_INTEGRITY FALSE")
                try {
                    for (table in tables) {
                        table.deleteAll()
                    }
                } finally {
                    exec("SET REFERENTIAL_INTEGRITY TRUE")
                }
            }
        }
    }

    afterSpec {
        DatabaseUtil.dropTables(*tables)
    }
}


/**
 * Registers hooks to use all database tables in tests.
 * - before spec, connects to DB and creates all tables.
 * - after each, cleans all entries in all tables, if cleanAfterEach is set to true.
 * - after spec, drops all tables.
 *
 * テスト中に (全ての) テーブルを使用するための Lifecycle hooks を登録します。
 * - before spec ... DB に接続し、テーブルを作成
 * - after each ... cleanAfterEach が true の場合、全てのテーブルの全てのエントリを削除
 * - after spec ... 全てのテーブルをドロップ
 *
 * Example:
 * ```
 * class KVStoreTest : FunSpec({
 *     useAllTables()
 *     // ...rest of the test...
 * })
 * ```
 *
 * @param cleanAfterEach Whether to clean all entries in the tables. Defaults to true.
 */
fun AbstractSpec.useAllTables(cleanAfterEach: Boolean = true) {
    beforeSpec {
        DatabaseUtil.connect(TEST_DB_MEM_URL)
        DatabaseUtil.createAllTables()
    }

    afterEach {
        if (cleanAfterEach) {
            transaction {
                exec("SET REFERENTIAL_INTEGRITY FALSE")
                try {
                    for (table in DatabaseUtil.tables) {
                        table.deleteAll()
                    }
                } finally {
                    exec("SET REFERENTIAL_INTEGRITY TRUE")
                }
            }
        }
    }

    afterSpec {
        DatabaseUtil.dropAllTables()
    }
}
