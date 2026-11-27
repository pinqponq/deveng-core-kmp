package core.data.temp

/**
 * Platform file operations for temp storage.
 * Implementations use java.io (JVM) or platform APIs (Native).
 */
expect class TempFileOps() {

    fun ensureDir(path: String)

    fun writeFile(path: String, bytes: ByteArray)

    fun readFile(path: String): ByteArray?

    fun deleteFile(path: String)

    fun fileExists(path: String): Boolean
}
