package global.deveng.core

/**
 * Returns a new unique path for saving a captured photo.
 * Used with [core.util.image.PhotoSaveUtils.savePhoto].
 */
expect fun getNewPhotoSavePath(): String
