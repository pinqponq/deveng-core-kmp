package core.util.image

/**
 * Debug: JPEG stored dimensions from SOF markers (no full decode).
 * Stored width×height may differ from on-screen aspect when EXIF orientation rotates the image.
 */
object JpegDebugProbe {

    private val sofMarkers: Set<Int> = setOf(
        0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7,
        0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF,
    )

    fun describe(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "empty"
        if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) {
            return "notJpeg bytes=${bytes.size}"
        }
        var i = 2
        while (i + 1 < bytes.size) {
            if (bytes[i] != 0xFF.toByte()) {
                i++
                continue
            }
            while (i < bytes.size && bytes[i] == 0xFF.toByte()) {
                i++
            }
            if (i >= bytes.size) break
            val marker = bytes[i].toInt() and 0xFF
            i++
            when {
                marker == 0xD9 -> return "jpeg eoi bytes=${bytes.size}"
                marker in 0xD0..0xD7 -> continue
                marker == 0xDA -> return "jpeg SOS(noSofYet) bytes=${bytes.size}"
                else -> {
                    if (i + 1 >= bytes.size) break
                    val segLen = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
                    if (segLen < 2 || i + segLen > bytes.size) {
                        return "jpeg badSeg marker=0x${marker.toString(16)} segLen=$segLen bytes=${bytes.size}"
                    }
                    if (marker in sofMarkers && segLen >= 9) {
                        val h = ((bytes[i + 3].toInt() and 0xFF) shl 8) or (bytes[i + 4].toInt() and 0xFF)
                        val w = ((bytes[i + 5].toInt() and 0xFF) shl 8) or (bytes[i + 6].toInt() and 0xFF)
                        val mp = (w.toLong() * h.toLong()) / 1_000_000.0
                        val mpStr = (kotlin.math.round(mp * 100.0) / 100.0).toString()
                        return "jpeg SOF=0x${marker.toString(16)} stored=${w}x${h} ~${mpStr}MP bytes=${bytes.size}"
                    }
                    i += segLen
                }
            }
        }
        return "jpeg noSOF bytes=${bytes.size}"
    }
}
