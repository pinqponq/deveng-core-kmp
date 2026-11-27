package global.deveng.core

import core.domain.temp.TempFileRepository

/**
 * Provides a platform-specific [TempFileRepository] for temporary file storage.
 * @param applicationContext Optional app context (e.g. Android Context); ignored on other platforms.
 */
expect fun getTempFileRepository(applicationContext: Any?): TempFileRepository
