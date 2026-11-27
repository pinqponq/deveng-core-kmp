package core.domain.temp

/**
 * Represents a file stored in app temp storage (persists across process death).
 * Use for any temporary byte storage (e.g. camera photos, documents). [id] is a unique key;
 * [fileName] is the file name under the temp directory.
 */
data class TempFileItem(
    val id: String,
    val fileName: String
)
