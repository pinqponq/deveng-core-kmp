package core.domain.camera.compose

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.util.Log
import core.domain.camera.controller.CameraController

/**
 * Transparent overlay View that handles pinch-to-zoom and double-tap.
 * Tap / double-tap timing uses [DOUBLE_TAP_MAX_INTERVAL_SEC] (same as iOS native preview) instead of
 * [ViewConfiguration.getDoubleTapTimeout] so both platforms match.
 */
internal class CameraZoomGestureView(
    context: Context,
    private var controller: CameraController,
    private var onZoomChange: (Float) -> Unit,
    private var onDoubleTap: () -> Unit,
    private var onFocusPointTapped: (Float, Float) -> Unit,
) : View(context) {

    private companion object {
        /** Same as iOS [CameraController.native] preview tap window. */
        private const val DOUBLE_TAP_MAX_INTERVAL_SEC = 0.20
    }

    private val doubleTapMaxIntervalMs = (DOUBLE_TAP_MAX_INTERVAL_SEC * 1000.0).toLong()
    private val touchSlopSq: Float
    private val mainHandler = Handler(Looper.getMainLooper())
    private var previewTapSequence = 0L
    private var lastPreviewTapTimeMs = 0L
    private var lastPreviewTapNx = 0f
    private var lastPreviewTapNy = 0f
    private var pendingSingleTapRunnable: Runnable? = null

    private var tapDownX = 0f
    private var tapDownY = 0f
    private var tapDownEventTime = 0L
    private var tapMovedBeyondSlop = false

    init {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        touchSlopSq = (slop * slop).toFloat()
    }

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val current = controller.getZoom()
                val max = controller.getMaxZoom().coerceAtLeast(1f)
                val newZoom = (current * detector.scaleFactor).coerceIn(1f, max)
                Log.d(
                    "CameraK-ZoomDebug",
                    "gestureScale current=$current factor=${detector.scaleFactor} max=$max new=$newZoom",
                )
                controller.setZoom(newZoom)
                val applied = controller.getZoom()
                Log.d("CameraK-ZoomDebug", "gestureScale applied=$applied")
                onZoomChange(applied)
                return true
            }
        },
    )

    private fun cancelPendingSingleTap() {
        pendingSingleTapRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingSingleTapRunnable = null
    }

    private fun invalidateTapSchedule() {
        previewTapSequence++
        cancelPendingSingleTap()
    }

    private fun normalizedTapPoint(event: MotionEvent): Pair<Float, Float>? {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return null
        val nx = (event.x / w).coerceIn(0f, 1f)
        val ny = (event.y / h).coerceIn(0f, 1f)
        return nx to ny
    }

    private fun onSingleTapConfirmedAt(nx: Float, ny: Float, tapEventTimeMs: Long) {
        val now = tapEventTimeMs
        val prev = lastPreviewTapTimeMs
        if (prev > 0L && now - prev <= doubleTapMaxIntervalMs) {
            invalidateTapSchedule()
            lastPreviewTapTimeMs = 0L
            onDoubleTap()
            return
        }
        lastPreviewTapTimeMs = now
        lastPreviewTapNx = nx
        lastPreviewTapNy = ny
        previewTapSequence++
        val scheduledSeq = previewTapSequence
        cancelPendingSingleTap()
        val runnable = Runnable {
            if (scheduledSeq != previewTapSequence) return@Runnable
            lastPreviewTapTimeMs = 0L
            if (controller.shouldSuppressTapToFocus?.invoke(lastPreviewTapNx, lastPreviewTapNy) == true) {
                return@Runnable
            }
            controller.setFocusPoint(lastPreviewTapNx, lastPreviewTapNy)
            onFocusPointTapped(lastPreviewTapNx, lastPreviewTapNy)
        }
        pendingSingleTapRunnable = runnable
        mainHandler.postDelayed(runnable, doubleTapMaxIntervalMs)
    }

    private fun handleTapPointerEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) {
                    tapDownX = event.x
                    tapDownY = event.y
                    tapDownEventTime = event.eventTime
                    tapMovedBeyondSlop = false
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                tapMovedBeyondSlop = true
                invalidateTapSchedule()
                lastPreviewTapTimeMs = 0L
            }
            MotionEvent.ACTION_MOVE -> {
                if (!tapMovedBeyondSlop && event.pointerCount == 1) {
                    val dx = event.x - tapDownX
                    val dy = event.y - tapDownY
                    if (dx * dx + dy * dy > touchSlopSq) {
                        tapMovedBeyondSlop = true
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (event.pointerCount != 1 || scaleGestureDetector.isInProgress) return
                if (tapMovedBeyondSlop) return
                val duration = event.eventTime - tapDownEventTime
                if (duration > ViewConfiguration.getLongPressTimeout()) return
                val pair = normalizedTapPoint(event) ?: return
                onSingleTapConfirmedAt(pair.first, pair.second, event.eventTime)
            }
            MotionEvent.ACTION_CANCEL -> {
                tapMovedBeyondSlop = true
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        handleTapPointerEvent(event)
        return true
    }

    fun update(
        newController: CameraController,
        newOnZoomChange: (Float) -> Unit,
        newOnDoubleTap: () -> Unit,
        newOnFocusPointTapped: (Float, Float) -> Unit,
    ) {
        controller = newController
        onZoomChange = newOnZoomChange
        onDoubleTap = newOnDoubleTap
        onFocusPointTapped = newOnFocusPointTapped
    }
}
