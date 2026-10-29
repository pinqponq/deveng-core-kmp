package core.domain.upload.model

import kotlinx.serialization.Serializable

/**
 * A large file that goes to storage in parts (an S3 or R2 multipart upload): every part is its own
 * presigned PUT, and storage answers each one with an ETag. The list of stored parts is persisted
 * in the descriptor after every part, so an upload interrupted by an app kill continues from the
 * first part that is not stored yet instead of starting over.
 *
 * [remoteKey] is what the app's server uses to find this upload when it signs a part or completes
 * the upload (for example its upload session id). Parts are numbered from 1, like S3 numbers them.
 */
@Serializable
data class PartedUpload(
    val remoteKey: String,
    val filePath: String,
    val totalBytes: Long,
    val partSizeBytes: Long,
    val storedParts: List<StoredPart> = emptyList(),
) {
    init {
        require(totalBytes > 0) { "A parted upload needs at least one byte, totalBytes=$totalBytes" }
        require(partSizeBytes > 0) { "The part size must be positive, partSizeBytes=$partSizeBytes" }
    }

    val partCount: Int get() = ((totalBytes + partSizeBytes - 1) / partSizeBytes).toInt()

    val storedBytes: Long get() = storedParts.sumOf { part -> byteRangeOf(part.partNumber).length }

    /** The lowest part number that is not stored yet; null when every part is stored. */
    fun nextPendingPartNumber(): Int? =
        (FIRST_PART_NUMBER..partCount).firstOrNull { partNumber -> !isStored(partNumber) }

    fun isStored(partNumber: Int): Boolean = storedParts.any { part -> part.partNumber == partNumber }

    fun byteRangeOf(partNumber: Int): PartByteRange {
        require(partNumber in FIRST_PART_NUMBER..partCount) { "Part $partNumber is outside 1..$partCount" }
        val offset = (partNumber - FIRST_PART_NUMBER) * partSizeBytes
        val length = minOf(partSizeBytes, totalBytes - offset)
        return PartByteRange(offset = offset, length = length)
    }

    /** Records [part] as stored; a part stored again replaces its earlier ETag (S3 keeps the last upload). */
    fun withStoredPart(part: StoredPart): PartedUpload {
        byteRangeOf(part.partNumber)
        val otherParts = storedParts.filter { storedPart -> storedPart.partNumber != part.partNumber }
        return copy(storedParts = (otherParts + part).sortedBy { storedPart -> storedPart.partNumber })
    }

    companion object {
        const val FIRST_PART_NUMBER = 1
    }
}

/** A part that storage accepted; [entityTag] is the ETag response header, which completing the upload needs. */
@Serializable
data class StoredPart(
    val partNumber: Int,
    val entityTag: String,
)

data class PartByteRange(
    val offset: Long,
    val length: Long,
)
