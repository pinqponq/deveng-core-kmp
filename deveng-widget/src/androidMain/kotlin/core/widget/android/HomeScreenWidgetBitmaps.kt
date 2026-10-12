package core.widget.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import co.touchlab.kermit.Logger
import core.widget.domain.image.HomeScreenWidgetImageSampling
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Longest side the stored widget image is shrunk to, enough for a widget resized to full screen width.
 * decodeImageForWidget still samples it down to the RemoteViews budget when it is drawn.
 */
private const val STORED_IMAGE_MAX_SIDE_PX = 1440
private const val STORED_IMAGE_JPEG_QUALITY = 85

/**
 * RemoteViews refuses an update whose bitmaps exceed about 1.5 × screen width × height × 4 bytes, and the
 * whole widget update crashes. Only half of that is used, leaving room for the launcher's own bitmaps.
 */
private const val REMOTE_VIEWS_BITMAP_LIMIT_SCREEN_FACTOR = 1.5
private const val REMOTE_VIEWS_BITMAP_BUDGET_SHARE = 0.5
private const val ARGB_BYTES_PER_PIXEL = 4

object HomeScreenWidgetBitmaps {

    /** Shrinks a downloaded image before storing it, so even large or portrait photos stay small on disk. */
    fun shrinkImageForStorage(imageBytes: ByteArray): ByteArray {
        val imageBounds = readBounds { options -> BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options) }
        val sampleSize = HomeScreenWidgetImageSampling.calculateSampleSizeForMaxSide(
            width = imageBounds.outWidth,
            height = imageBounds.outHeight,
            maxSidePx = STORED_IMAGE_MAX_SIDE_PX,
        )
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampledBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
            ?: throw IllegalArgumentException("The widget image could not be decoded.")

        val storedBitmap = scaleToMaxSide(sampledBitmap, STORED_IMAGE_MAX_SIDE_PX)
        val jpegOutputStream = ByteArrayOutputStream()
        storedBitmap.compress(Bitmap.CompressFormat.JPEG, STORED_IMAGE_JPEG_QUALITY, jpegOutputStream)
        if (storedBitmap !== sampledBitmap) sampledBitmap.recycle()
        storedBitmap.recycle()
        return jpegOutputStream.toByteArray()
    }

    /**
     * Decodes the stored image small enough to stay inside the RemoteViews bitmap budget of this screen.
     * A responsive widget carries the bitmap once per layout size, so the budget is split between them.
     */
    fun decodeImageForWidget(context: Context, imageFile: File, layoutCount: Int): Bitmap? {
        if (!imageFile.exists()) return null

        val imageBounds = readBounds { options -> BitmapFactory.decodeFile(imageFile.path, options) }
        val bitmapBudgetBytes = calculateRemoteViewsBitmapBudgetBytes(context) / layoutCount.coerceAtLeast(1)
        val sampleSize = HomeScreenWidgetImageSampling.calculateSampleSizeForByteBudget(
            width = imageBounds.outWidth,
            height = imageBounds.outHeight,
            budgetBytes = bitmapBudgetBytes,
        )

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val widgetBitmap = BitmapFactory.decodeFile(imageFile.path, decodeOptions)
        if (widgetBitmap == null) {
            Logger.w { "[HomeScreenWidget] Stored widget image could not be decoded path=${imageFile.path}" }
        }

        return widgetBitmap
    }

    private fun readBounds(decodeBounds: (BitmapFactory.Options) -> Unit): BitmapFactory.Options {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decodeBounds(boundsOptions)
        return boundsOptions
    }

    private fun scaleToMaxSide(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (longestSide <= maxSidePx) return bitmap

        val scale = maxSidePx.toFloat() / longestSide
        val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }

    private fun calculateRemoteViewsBitmapBudgetBytes(context: Context): Long {
        val displayMetrics = context.resources.displayMetrics
        val screenBytes = displayMetrics.widthPixels.toLong() * displayMetrics.heightPixels * ARGB_BYTES_PER_PIXEL
        return (screenBytes * REMOTE_VIEWS_BITMAP_LIMIT_SCREEN_FACTOR * REMOTE_VIEWS_BITMAP_BUDGET_SHARE).toLong()
    }
}
