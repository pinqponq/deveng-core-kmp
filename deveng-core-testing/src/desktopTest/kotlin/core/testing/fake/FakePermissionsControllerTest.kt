package core.testing.fake

import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakePermissionsControllerTest {

    private val permissionsController = FakePermissionsController()

    @Test
    fun getPermissionState_withoutConfiguredState_returnsNotDetermined() = runTest {
        assertEquals(PermissionState.NotDetermined, permissionsController.getPermissionState(Permission.CAMERA))
        assertFalse(permissionsController.isPermissionGranted(Permission.CAMERA))
    }

    @Test
    fun providePermission_withoutFailure_grantsPermission() = runTest {
        permissionsController.providePermission(Permission.CAMERA)

        assertTrue(permissionsController.isPermissionGranted(Permission.CAMERA))
        assertEquals(mapOf(Permission.CAMERA to PermissionState.Granted), permissionsController.permissionStates)
    }

    @Test
    fun providePermission_withFailure_throwsAndAppliesStateAfterFailure() = runTest {
        val failure = IllegalStateException("denied")
        permissionsController.providePermissionFailures[Permission.CAMERA] = failure
        permissionsController.permissionStatesAfterFailure[Permission.CAMERA] = PermissionState.DeniedAlways

        val thrownFailure = assertFailsWith<IllegalStateException> { permissionsController.providePermission(Permission.CAMERA) }

        assertEquals(failure, thrownFailure)
        assertEquals(PermissionState.DeniedAlways, permissionsController.getPermissionState(Permission.CAMERA))
    }

    @Test
    fun openSettings_calledDifferentTimes_countsEachSeparately() {
        permissionsController.openAppSettings()
        permissionsController.openNotificationSettings()
        permissionsController.openNotificationSettings()

        assertEquals(1, permissionsController.openAppSettingsCount)
        assertEquals(2, permissionsController.openNotificationSettingsCount)
    }
}
