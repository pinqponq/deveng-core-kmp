package global.deveng.core

import android.content.Context
import core.data.temp.AndroidTempStorageDirProvider
import core.data.temp.TempFileOps
import core.data.temp.TempFileRepositoryImpl
import core.domain.temp.TempFileRepository

actual fun getTempFileRepository(applicationContext: Any?): TempFileRepository {
    val context = applicationContext as? Context
        ?: throw IllegalStateException("Android requires Context for getTempFileRepository")
    val dirProvider = AndroidTempStorageDirProvider(context, "sample_temp")
    val fileOps = TempFileOps()
    return TempFileRepositoryImpl(dirProvider, fileOps)
}
