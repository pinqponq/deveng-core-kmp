package com.sekompos.huhuvadmin.data.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import global.deveng.R
import java.io.File

class ComposeFileProvider : FileProvider(
    R.xml.path_provider
) {
    companion object {
        fun getImageUri(context: Context): Uri {
            val tempFile = File.createTempFile(
                "picture_${System.currentTimeMillis()}", ".png", context.cacheDir
            ).apply {
                createNewFile()
            }
            val authority = context.applicationContext.packageName + ".provider"
            return getUriForFile(
                context,
                authority,
                tempFile
            )
        }

    }

}
