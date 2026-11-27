package core.domain.temp

/**
 * Persistent temp storage for byte files (survives app process death).
 * Use for any use case that needs to stack or hold files temporarily (e.g. camera photos,
 * documents) before upload or processing. The storage path is defined by [core.data.temp.TempStorageDirProvider].
 */
interface TempFileRepository {

    /**
     * Saves bytes to temp storage and returns the created item.
     * @param fileExtension File extension for the stored file (default "jpg"). Include the dot if desired (e.g. ".pdf"); implementation will normalize.
     */
    suspend fun saveBytes(byteArray: ByteArray, fileExtension: String = "jpg"): TempFileItem

    /**
     * Returns all stored items in order (oldest first).
     */
    suspend fun loadAll(): List<TempFileItem>

    /**
     * Returns the number of stored items.
     */
    suspend fun getCount(): Int

    /**
     * Loads bytes for a stored item, or null if missing.
     */
    suspend fun loadBytes(item: TempFileItem): ByteArray?

    /**
     * Replaces bytes of an existing item while preserving its id/order metadata.
     * @return true if item exists and bytes are updated, false otherwise.
     */
    suspend fun updateBytes(itemId: String, byteArray: ByteArray): Boolean

    /**
     * Removes the item from temp storage. Idempotent if already missing.
     */
    suspend fun delete(itemId: String)
}
