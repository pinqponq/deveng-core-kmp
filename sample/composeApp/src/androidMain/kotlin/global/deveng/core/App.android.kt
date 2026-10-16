package global.deveng.core

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import core.util.image.PhotoSaveUtils
import core.util.video.VideoSaveUtils

class AppActivity : ComponentActivity() {
    private val plateRecognitionModelsViewModel: PlateRecognitionModelsViewModel by lazy {
        ViewModelProvider(this)[PlateRecognitionModelsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(
            PHASE0_LOG_TAG,
            "event=activity_on_create pid=${android.os.Process.myPid()} " +
                "timestampNanos=${System.nanoTime()}",
        )
        enableEdgeToEdge()
        PhotoSaveUtils.setApplicationContext(applicationContext)
        VideoSaveUtils.setApplicationContext(applicationContext)
        val storedModelsFound = plateRecognitionModelsViewModel.prepareStoredModels(
            filesDirectory = applicationContext.filesDir,
        )
        Log.i(
            PHASE0_LOG_TAG,
            "event=app_model_preparation_request modelsFound=$storedModelsFound " +
                "timestampNanos=${System.nanoTime()}",
        )
        setContent {
            var showPlateRecognitionModelPreparation by remember { mutableStateOf(false) }
            var showPlateRecognitionValidation by remember { mutableStateOf(false) }
            App(
                onOpenPlateRecognitionModelPreparation = {
                    showPlateRecognitionModelPreparation = true
                },
                plateRecognitionModelPreparationContent = if (
                    showPlateRecognitionModelPreparation
                ) {
                    {
                        PlateRecognitionModelPreparationScreen(
                            modelsViewModel = plateRecognitionModelsViewModel,
                            onBack = { showPlateRecognitionModelPreparation = false },
                            onOpenValidation = {
                                showPlateRecognitionModelPreparation = false
                                showPlateRecognitionValidation = true
                            },
                        )
                    }
                } else {
                    null
                },
                plateRecognitionValidationContent = if (showPlateRecognitionValidation) {
                    {
                        PlateRecognitionValidationScreen(
                            modelsViewModel = plateRecognitionModelsViewModel,
                            onBack = { showPlateRecognitionValidation = false },
                        )
                    }
                } else {
                    null
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(
            PHASE0_LOG_TAG,
            "event=activity_on_resume pid=${android.os.Process.myPid()} " +
                "timestampNanos=${System.nanoTime()}",
        )
    }

    private companion object {
        const val PHASE0_LOG_TAG = "PlateRuntimePhase0"
    }
}
