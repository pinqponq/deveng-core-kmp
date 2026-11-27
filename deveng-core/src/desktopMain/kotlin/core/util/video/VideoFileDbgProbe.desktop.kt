package core.util.video

actual object VideoFileDbgProbe {
    actual fun describe(filePath: String): String = "path=$filePath (desktop probe n/a)"
}
