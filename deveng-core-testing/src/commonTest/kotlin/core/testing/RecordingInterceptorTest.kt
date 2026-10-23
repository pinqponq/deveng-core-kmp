package core.testing

import core.presentation.baseviewmodel.ErrorDecision
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewSideEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingInterceptorTest {

    @Test
    fun onError_withDefaultDecision_returnsShow() {
        val recordingInterceptor = RecordingInterceptor()

        val errorDecision = recordingInterceptor.onError(viewModelName = VIEW_MODEL_NAME, exception = FAILURE)

        assertEquals(ErrorDecision.SHOW, errorDecision)
    }

    @Test
    fun onError_withConfiguredDecision_returnsIt() {
        val recordingInterceptor = RecordingInterceptor(errorDecision = ErrorDecision.SILENT)

        val errorDecision = recordingInterceptor.onError(viewModelName = VIEW_MODEL_NAME, exception = FAILURE)

        assertEquals(ErrorDecision.SILENT, errorDecision)
    }

    @Test
    fun clear_afterEveryKindOfCall_emptiesEveryRecording() {
        val recordingInterceptor = RecordingInterceptor()
        recordingInterceptor.onEvent(viewModelName = VIEW_MODEL_NAME, event = object : ViewEvent {})
        recordingInterceptor.onEffect(viewModelName = VIEW_MODEL_NAME, effect = object : ViewSideEffect {})
        recordingInterceptor.onError(viewModelName = VIEW_MODEL_NAME, exception = FAILURE)
        recordingInterceptor.onSilentError(viewModelName = VIEW_MODEL_NAME, exception = FAILURE)

        recordingInterceptor.clear()

        assertTrue(recordingInterceptor.recordedEvents.isEmpty())
        assertTrue(recordingInterceptor.recordedEffects.isEmpty())
        assertTrue(recordingInterceptor.recordedErrors.isEmpty())
        assertTrue(recordingInterceptor.recordedSilentErrors.isEmpty())
    }

    private companion object {
        const val VIEW_MODEL_NAME = "ProfileViewModel"
        val FAILURE = IllegalStateException("profile failed")
    }
}
