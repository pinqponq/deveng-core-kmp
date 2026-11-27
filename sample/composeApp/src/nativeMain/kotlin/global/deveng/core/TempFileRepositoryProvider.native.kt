package global.deveng.core

import core.data.temp.IosTempStorageDirProvider
import core.data.temp.TempFileOps
import core.data.temp.TempFileRepositoryImpl
import core.domain.temp.TempFileRepository

actual fun getTempFileRepository(applicationContext: Any?): TempFileRepository {
    val dirProvider = IosTempStorageDirProvider("sample_temp")
    val fileOps = TempFileOps()
    return TempFileRepositoryImpl(dirProvider, fileOps)
}
