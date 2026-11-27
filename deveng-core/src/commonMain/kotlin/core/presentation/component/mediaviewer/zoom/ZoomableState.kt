package core.presentation.component.mediaviewer.zoom

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.IntSize

@Composable
fun rememberZoomableState(
    config: ZoomableConfig = ZoomableConfig(),
    resetKey: Any? = null,
): ZoomableState {
    val state = remember(config) { ZoomableState(config) }
    if (resetKey != null) {
        var lastResetKey by remember { mutableStateOf<Any?>(null) }
        LaunchedEffect(resetKey) {
            if (lastResetKey != null && lastResetKey != resetKey) state.resetZoom()
            lastResetKey = resetKey
        }
    }
    return state
}

@Stable
class ZoomableState internal constructor(val config: ZoomableConfig) {

    private var _scale by mutableStateOf(config.minZoom)
    private var _offset by mutableStateOf(Offset.Zero)
    private var _isAnimating by mutableStateOf(false)
    private var _layoutSize by mutableStateOf(IntSize.Zero)

    private val scaleAnimatable = Animatable(_scale)
    private val offsetAnimatable = Animatable(_offset, Offset.VectorConverter)

    val layoutSize: IntSize get() = _layoutSize

    val transformation: ContentTransformation by derivedStateOf {
        ContentTransformation(scale = ScaleFactor(_scale, _scale), offset = _offset)
    }

    val zoomFraction: Float by derivedStateOf {
        if (config.maxZoom <= config.minZoom) 0f
        else ((_scale - config.minZoom) / (config.maxZoom - config.minZoom)).coerceIn(0f, 1f)
    }

    val isZoomed: Boolean by derivedStateOf { _scale > config.minZoom }

    val isAnimating: Boolean get() = _isAnimating

    internal val scale: Float get() = _scale
    internal val offset: Offset get() = _offset

    suspend fun zoomTo(
        scale: Float,
        centroid: Offset = Offset.Unspecified,
        animationSpec: AnimationSpec<Float> = spring(),
    ) {
        val targetScale = scale.coerceIn(config.minZoom, config.maxZoom)
        if (_scale == targetScale) return
        _isAnimating = true
        try {
            val targetOffset = calculateZoomOffset(_offset, _scale, targetScale, centroid)
            val startScale = _scale
            val startOffset = _offset
            scaleAnimatable.snapTo(0f)
            scaleAnimatable.animateTo(1f, animationSpec) {
                val p = value
                _scale = startScale + (targetScale - startScale) * p
                _offset = Offset(
                    x = startOffset.x + (targetOffset.x - startOffset.x) * p,
                    y = startOffset.y + (targetOffset.y - startOffset.y) * p,
                )
            }
            _scale = targetScale
            _offset = targetOffset
            constrainOffset()
        } finally {
            _isAnimating = false
        }
    }

    suspend fun zoomBy(
        zoomFactor: Float,
        centroid: Offset = Offset.Unspecified,
        animationSpec: AnimationSpec<Float> = spring(),
    ) = zoomTo(_scale * zoomFactor, centroid, animationSpec)

    suspend fun panBy(
        offset: Offset,
        animationSpec: AnimationSpec<Float> = spring(),
    ) {
        val targetOffset = constrainOffset(_offset + offset)
        _isAnimating = true
        try {
            offsetAnimatable.animateTo(targetOffset, spring()) { _offset = value }
        } finally {
            _isAnimating = false
        }
    }

    suspend fun resetZoom(animationSpec: AnimationSpec<Float> = spring()) {
        _isAnimating = true
        try {
            val startScale = _scale
            val startOffset = _offset
            scaleAnimatable.snapTo(0f)
            scaleAnimatable.animateTo(1f, animationSpec) {
                val p = value
                _scale = startScale + (config.minZoom - startScale) * p
                _offset = Offset(x = startOffset.x * (1f - p), y = startOffset.y * (1f - p))
            }
            _scale = config.minZoom
            _offset = Offset.Zero
        } finally {
            _isAnimating = false
        }
    }

    internal fun setLayoutSize(size: IntSize) {
        _layoutSize = size
        constrainOffset()
    }

    internal fun onGestureZoom(zoomChange: Float, centroid: Offset) {
        val newScale = (_scale * zoomChange).coerceIn(config.minZoom, config.maxZoom)
        if (_scale == newScale) return
        _offset = calculateZoomOffset(_offset, _scale, newScale, centroid)
        _scale = newScale
        constrainOffset()
    }

    internal fun onGesturePan(pan: Offset): Offset {
        val oldOffset = _offset
        _offset = constrainOffset(_offset + pan)
        return _offset - oldOffset
    }

    private fun calculateZoomOffset(
        currentOffset: Offset,
        currentScale: Float,
        targetScale: Float,
        centroid: Offset,
    ): Offset {
        if (centroid == Offset.Unspecified || _layoutSize == IntSize.Zero) return currentOffset
        val centerX = _layoutSize.width / 2f
        val centerY = _layoutSize.height / 2f
        val centroidFromCenter = Offset(centroid.x - centerX, centroid.y - centerY)
        val scaleRatio = targetScale / currentScale
        return (currentOffset - centroidFromCenter) * scaleRatio + centroidFromCenter
    }

    private fun constrainOffset(offset: Offset): Offset {
        if (_layoutSize == IntSize.Zero) return offset
        val maxOffsetX = (_layoutSize.width * (_scale - 1f) / 2f).coerceAtLeast(0f)
        val maxOffsetY = (_layoutSize.height * (_scale - 1f) / 2f).coerceAtLeast(0f)
        return Offset(
            x = offset.x.coerceIn(-maxOffsetX, maxOffsetX),
            y = offset.y.coerceIn(-maxOffsetY, maxOffsetY),
        )
    }

    private fun constrainOffset() {
        _offset = constrainOffset(_offset)
    }
}
