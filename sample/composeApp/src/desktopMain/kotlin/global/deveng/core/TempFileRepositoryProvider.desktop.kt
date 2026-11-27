package global.deveng.core

import core.data.temp.DesktopTempStorageDirProvider
import core.data.temp.TempFileOps
import core.data.temp.TempFileRepositoryImpl
import core.domain.temp.TempFileRepository

actual fun getTempFileRepository(applicationContext: Any?): TempFileRepository {
    val dirProvider = DesktopTempStorageDirProvider.forApp("deveng", "sample_temp")
    val fileOps = TempFileOps()
    return TempFileRepositoryImpl(dirProvider, fileOps)
}
