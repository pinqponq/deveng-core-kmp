package com.deveng.calf.picker.platform

internal interface NativeDialogs {
    fun init()

    fun createFileDialog(
        title: String?,
        initialDirectory: String?,
        extensions: Array<String>?,
        multiple: Boolean,
        parentWindow: Long,
    ): Long

    fun createDirectoryDialog(title: String?, initialDirectory: String?, parentWindow: Long): Long

    fun createSaveDialog(
        title: String?,
        initialDirectory: String?,
        defaultName: String?,
        extension: String?,
        parentWindow: Long,
    ): Long

    fun showFileDialog(handle: Long): Array<String>

    fun showDirectoryDialog(handle: Long): String?

    fun showSaveDialog(handle: Long): String?

    fun destroyDialog(handle: Long)
}

internal object JniNativeDialogs : NativeDialogs {
    override fun init() = NativeFilePickerBridge.init()

    override fun createFileDialog(
        title: String?,
        initialDirectory: String?,
        extensions: Array<String>?,
        multiple: Boolean,
        parentWindow: Long,
    ): Long = NativeFilePickerBridge.createFileDialog(title, initialDirectory, extensions, multiple, parentWindow)

    override fun createDirectoryDialog(title: String?, initialDirectory: String?, parentWindow: Long): Long =
        NativeFilePickerBridge.createDirectoryDialog(title, initialDirectory, parentWindow)

    override fun createSaveDialog(
        title: String?,
        initialDirectory: String?,
        defaultName: String?,
        extension: String?,
        parentWindow: Long,
    ): Long = NativeFilePickerBridge.createSaveDialog(title, initialDirectory, defaultName, extension, parentWindow)

    override fun showFileDialog(handle: Long): Array<String> = NativeFilePickerBridge.showFileDialog(handle)

    override fun showDirectoryDialog(handle: Long): String? = NativeFilePickerBridge.showDirectoryDialog(handle)

    override fun showSaveDialog(handle: Long): String? = NativeFilePickerBridge.showSaveDialog(handle)

    override fun destroyDialog(handle: Long) = NativeFilePickerBridge.destroyDialog(handle)
}
