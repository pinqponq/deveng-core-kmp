package global.deveng.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import core.util.image.PhotoSaveUtils
import core.util.video.VideoSaveUtils

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PhotoSaveUtils.setApplicationContext(applicationContext)
        VideoSaveUtils.setApplicationContext(applicationContext)
        setContent { App() }
    }
}
