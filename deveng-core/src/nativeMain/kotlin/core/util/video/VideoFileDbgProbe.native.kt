package core.util.video

actual object VideoFileDbgProbe {
    actual fun describe(filePath: String): String = "path=$filePath (native probe n/a)"
}
