package core.util.video

/**
 * Local video file metadata for resolution triage (recording → review → gallery export).
 */
expect object VideoFileDbgProbe {
    fun describe(filePath: String): String
}
