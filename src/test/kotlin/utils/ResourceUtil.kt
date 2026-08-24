package utils

import java.io.File

object ResourceUtil {
    fun loadResourceFile(path: String): File {
        val resource = javaClass.getResource(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        return File(resource.toURI())
    }
}
