package core.data.temp

import java.io.File

actual class TempFileOps actual constructor() {

    actual fun ensureDir(path: String) {
        File(path).mkdirs()
    }

    actual fun writeFile(path: String, bytes: ByteArray) {
        File(path).writeBytes(bytes)
    }

    actual fun readFile(path: String): ByteArray? {
        val file = File(path)
        return if (file.exists()) file.readBytes() else null
    }

    actual fun deleteFile(path: String) {
        File(path).delete()
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
}
