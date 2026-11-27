package core.presentation.motion

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val DEFAULT_ANIMATOR_DURATION_SCALE = 1f

@Composable
actual fun rememberSystemReduceMotion(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    var isReduceMotionEnabled by remember(contentResolver) {
        mutableStateOf(readIsAnimatorDisabled(contentResolver))
    }

    DisposableEffect(contentResolver) {
        val animatorScaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isReduceMotionEnabled = readIsAnimatorDisabled(contentResolver)
            }
        }

        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            animatorScaleObserver
        )

        onDispose { contentResolver.unregisterContentObserver(animatorScaleObserver) }
    }

    return isReduceMotionEnabled
}

private fun readIsAnimatorDisabled(contentResolver: ContentResolver): Boolean {
    val animatorDurationScale = Settings.Global.getFloat(
        contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        DEFAULT_ANIMATOR_DURATION_SCALE
    )
    return animatorDurationScale == 0f
}
