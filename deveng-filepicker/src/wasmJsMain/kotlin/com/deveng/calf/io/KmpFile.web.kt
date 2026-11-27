package com.deveng.calf.io

import com.deveng.calf.core.PlatformContext
import org.w3c.files.File

actual class KmpFile(
    val file: File,
)

actual fun KmpFile.exists(context: PlatformContext) = true

actual suspend fun KmpFile.readByteArray(context: PlatformContext): ByteArray =
    readFileAsBytes(file)

actual fun KmpFile.getName(context: PlatformContext): String? = file.name

actual fun KmpFile.getPath(context: PlatformContext): String? = file.name

actual fun KmpFile.getSize(context: PlatformContext): Long? = null

actual fun KmpFile.isDirectory(context: PlatformContext): Boolean = !file.name.contains(".")

/**
 * Converts this [org.w3c.files.File] to a [KmpFile].
 */
fun File.toKmpFile(): KmpFile = KmpFile(this)
