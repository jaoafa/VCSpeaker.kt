package utils

object Constants {
    const val TEST_DB_MEM_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    const val TEST_DB_FILE_URL = "jdbc:h2:file:./test-database/h2;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"
}
