package global.deveng.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import core.util.image.PhotoSaveUtils
import core.util.video.VideoSaveUtils

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PhotoSaveUtils.setApplicationContext(applicationContext)
        VideoSaveUtils.setApplicationContext(applicationContext)
        setContent {
            var showNanoDetValidation by remember { mutableStateOf(false) }
            App(
                onOpenNanoDetValidation = { showNanoDetValidation = true },
                nanoDetValidationContent = if (showNanoDetValidation) {
                    { NanoDetValidationScreen(onBack = { showNanoDetValidation = false }) }
                } else {
                    null
                },
            )
        }
    }
}
