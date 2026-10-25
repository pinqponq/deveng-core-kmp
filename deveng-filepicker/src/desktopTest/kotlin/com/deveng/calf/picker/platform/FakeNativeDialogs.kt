package com.deveng.calf.picker.platform

internal class FakeNativeDialogs : NativeDialogs {
    val calls = mutableListOf<String>()
    var nextHandle = 7L
    var filePaths: Array<String> = emptyArray()
    var directoryPath: String? = null
    var savePath: String? = null
    var lastExtensions: Array<String>? = null
    var lastMultiple: Boolean? = null
    var lastTitle: String? = null
    var lastInitialDirectory: String? = null
    var lastDefaultName: String? = null
    var lastSaveExtension: String? = null
    var lastParentWindow: Long? = null

    override fun init() {
        calls += "init"
    }

    override fun createFileDialog(
        title: String?,
        initialDirectory: String?,
        extensions: Array<String>?,
        multiple: Boolean,
        parentWindow: Long,
    ): Long {
        calls += "createFile"
        lastTitle = title
        lastInitialDirectory = initialDirectory
        lastExtensions = extensions
        lastMultiple = multiple
        lastParentWindow = parentWindow
        return nextHandle
    }

    override fun createDirectoryDialog(title: String?, initialDirectory: String?, parentWindow: Long): Long {
        calls += "createDirectory"
        lastTitle = title
        lastInitialDirectory = initialDirectory
        lastParentWindow = parentWindow
        return nextHandle
    }

    override fun createSaveDialog(
        title: String?,
        initialDirectory: String?,
        defaultName: String?,
        extension: String?,
        parentWindow: Long,
    ): Long {
        calls += "createSave"
        lastTitle = title
        lastInitialDirectory = initialDirectory
        lastDefaultName = defaultName
        lastSaveExtension = extension
        lastParentWindow = parentWindow
        return nextHandle
    }

    override fun showFileDialog(handle: Long): Array<String> {
        calls += "showFile:$handle"
        return filePaths
    }

    override fun showDirectoryDialog(handle: Long): String? {
        calls += "showDirectory:$handle"
        return directoryPath
    }

    override fun showSaveDialog(handle: Long): String? {
        calls += "showSave:$handle"
        return savePath
    }

    override fun destroyDialog(handle: Long) {
        calls += "destroy:$handle"
    }
}
