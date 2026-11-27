package core.domain.camera.temp

import core.domain.temp.TempFileItem
import core.domain.temp.TempFileRepository

/**
 * Type alias for [TempFileRepository]. Use [core.domain.temp.TempFileRepository] and [TempFileRepository.saveBytes] for new code.
 * For camera photos, call [savePhoto] extension or [TempFileRepository.saveBytes] with fileExtension "jpg".
 */
@Deprecated("Use core.domain.temp.TempFileRepository", ReplaceWith("TempFileRepository", "core.domain.temp.TempFileRepository"))
typealias CameraTempPhotoRepository = TempFileRepository

/**
 * Saves byte array as a photo (jpg) and returns the created item.
 * Convenience for camera use case when using [CameraTempPhotoRepository] (TempFileRepository).
 */
suspend fun TempFileRepository.savePhoto(byteArray: ByteArray): TempFileItem = saveBytes(byteArray, "jpg")
