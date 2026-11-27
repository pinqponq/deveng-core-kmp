package core.data.temp

import core.domain.temp.TempFileItem
import core.util.IoDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists and reads the index of temp file items (id + fileName) in the temp directory.
 * Index file: [basePath]/index.json. Uses [TempFileOps] for platform file I/O.
 */
internal class TempFileIndex(
    private val basePath: String,
    private val fileOps: TempFileOps
) {
    private val indexPath: String
        get() = "$basePath/$INDEX_FILE_NAME"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(): List<IndexEntry> = withContext(IoDispatcher) {
        val bytes = fileOps.readFile(indexPath) ?: return@withContext emptyList()
        runCatching {
            json.decodeFromString<IndexFile>(bytes.decodeToString()).items
        }.getOrElse { emptyList() }
    }

    suspend fun write(entries: List<IndexEntry>) = withContext(IoDispatcher) {
        fileOps.ensureDir(basePath)
        fileOps.writeFile(indexPath, json.encodeToString(IndexFile(entries)).encodeToByteArray())
    }

    fun toTempFileItem(entry: IndexEntry) = TempFileItem(id = entry.id, fileName = entry.fileName)

    fun toEntry(item: TempFileItem) = IndexEntry(id = item.id, fileName = item.fileName)

    @Serializable
    data class IndexEntry(
        val id: String,
        val fileName: String
    )

    @Serializable
    private data class IndexFile(val items: List<IndexEntry>)

    companion object {
        private const val INDEX_FILE_NAME = "index.json"
    }
}
