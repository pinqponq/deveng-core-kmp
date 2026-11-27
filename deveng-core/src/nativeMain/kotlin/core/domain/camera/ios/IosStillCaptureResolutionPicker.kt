package core.domain.camera.ios

import platform.Foundation.NSLog

/**
 * Parses Swift-reported still sizes and picks the best 9:16 (portrait cap) match.
 */
object IosStillCaptureResolutionPicker {

    fun loadSupportedSizes(isFrontCamera: Boolean): List<Pair<Int, Int>> {
        val csv = IosCameraStillResolutionBridge.supportedSizesCsvProvider?.invoke(isFrontCamera)
            ?: return emptyList()
        if (csv.isBlank()) return emptyList()
        return csv.split(",").mapNotNull { token ->
            val parts = token.trim().split("x", "X")
            if (parts.size != 2) return@mapNotNull null
            val w = parts[0].toIntOrNull() ?: return@mapNotNull null
            val h = parts[1].toIntOrNull() ?: return@mapNotNull null
            if (w <= 0 || h <= 0) return@mapNotNull null
            w to h
        }.distinctBy { (w, h) -> w to h }
    }

    fun pickBestUnderCap(
        sizes: List<Pair<Int, Int>>,
        cap: Pair<Int, Int>,
    ): Pair<Int, Int>? {
        val shortCap = minOf(cap.first, cap.second)
        val longCap = maxOf(cap.first, cap.second)
        val targetAspect = 9.0 / 16.0
        val filtered = sizes.filter { (w, h) ->
            minOf(w, h) <= shortCap && maxOf(w, h) <= longCap
        }
        return filtered.sortedWith(
            compareBy<Pair<Int, Int>> { (w, h) ->
                val shortSide = minOf(w, h).toDouble()
                val longSide = maxOf(w, h).toDouble()
                kotlin.math.abs((shortSide / longSide) - targetAspect)
            }.thenByDescending { (w, h) ->
                w.toLong() * h
            }.thenByDescending { (w, h) ->
                if (h >= w) 1 else 0
            }.thenByDescending { (w, h) ->
                maxOf(w, h).toLong()
            },
        ).firstOrNull()
    }

    fun logAndPickBest(
        isFrontCamera: Boolean,
        cap: Pair<Int, Int>?,
    ): Pair<Int, Int>? {
        if (cap == null) return null
        val sizes = loadSupportedSizes(isFrontCamera)
        val best = pickBestUnderCap(sizes, cap)
        val lens = if (isFrontCamera) "front" else "back"
        NSLog(
            "CameraK",
            "iosStillSizes lens=$lens cap=$cap swiftCount=${sizes.size} " +
                "best=$best samples=${sizes.take(10).joinToString { "${it.first}x${it.second}" }}",
        )
        return best
    }
}
