package core.domain.camera.ios

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface

internal fun mirrorImageBitmapHorizontally(bitmap: ImageBitmap): ImageBitmap {
    val src = bitmap.asSkiaBitmap()
    val w = src.width
    val h = src.height
    val surface = Surface.makeRasterN32Premul(w, h)
    val canvas = surface.canvas
    val image = Image.makeFromBitmap(src)
    canvas.save()
    canvas.translate(w.toFloat(), 0f)
    canvas.scale(-1f, 1f)
    canvas.drawImage(image, 0f, 0f)
    canvas.restore()
    return surface.makeImageSnapshot().toComposeImageBitmap()
}
