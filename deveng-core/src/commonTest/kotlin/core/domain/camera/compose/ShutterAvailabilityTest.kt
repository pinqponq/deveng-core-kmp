package core.domain.camera.compose

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShutterAvailabilityTest {

    @Test
    fun isShutterUsable_withAnEnabledShutterOutsideSingleCapture_returnsTrue() {
        assertTrue(isShutterUsable(isShutterEnabled = true, singleCaptureModeEnabled = false, hasClaimedSingleCapture = true))
    }

    @Test
    fun isShutterUsable_withAnUnclaimedSingleCapture_returnsTrue() {
        assertTrue(isShutterUsable(isShutterEnabled = true, singleCaptureModeEnabled = true, hasClaimedSingleCapture = false))
    }

    @Test
    fun isShutterUsable_withAClaimedSingleCapture_returnsFalse() {
        assertFalse(isShutterUsable(isShutterEnabled = true, singleCaptureModeEnabled = true, hasClaimedSingleCapture = true))
    }

    @Test
    fun isShutterUsable_withADisabledShutter_returnsFalse() {
        assertFalse(isShutterUsable(isShutterEnabled = false, singleCaptureModeEnabled = false, hasClaimedSingleCapture = false))
    }

    @Test
    fun isShutterButtonEnabled_whileRecording_returnsTrueEvenWhenTheShutterIsDisabled() {
        assertTrue(
            isShutterButtonEnabled(
                isShutterEnabled = false,
                singleCaptureModeEnabled = true,
                hasClaimedSingleCapture = true,
                isRecording = true,
            ),
        )
    }

    @Test
    fun isShutterButtonEnabled_withADisabledShutterAndNoRecording_returnsFalse() {
        assertFalse(
            isShutterButtonEnabled(
                isShutterEnabled = false,
                singleCaptureModeEnabled = false,
                hasClaimedSingleCapture = false,
                isRecording = false,
            ),
        )
    }

    @Test
    fun isShutterButtonEnabled_withAUsableShutterAndNoRecording_returnsTrue() {
        assertTrue(
            isShutterButtonEnabled(
                isShutterEnabled = true,
                singleCaptureModeEnabled = false,
                hasClaimedSingleCapture = false,
                isRecording = false,
            ),
        )
    }
}
