package core.testing.fake

import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import core.presentation.permission.PermissionsController

class FakePermissionsController : PermissionsController {

    val permissionStates = mutableMapOf<Permission, PermissionState>()
    val providePermissionFailures = mutableMapOf<Permission, Exception>()
    val permissionStatesAfterFailure = mutableMapOf<Permission, PermissionState>()

    var openAppSettingsCount = 0
        private set

    var openNotificationSettingsCount = 0
        private set

    override suspend fun providePermission(permission: Permission) {
        providePermissionFailures[permission]?.let { failure ->
            permissionStatesAfterFailure[permission]?.let { state -> permissionStates[permission] = state }
            throw failure
        }

        permissionStates[permission] = PermissionState.Granted
    }

    override suspend fun isPermissionGranted(permission: Permission): Boolean =
        getPermissionState(permission = permission) == PermissionState.Granted

    override suspend fun getPermissionState(permission: Permission): PermissionState =
        permissionStates[permission] ?: PermissionState.NotDetermined

    override fun openAppSettings() {
        openAppSettingsCount++
    }

    override fun openNotificationSettings() {
        openNotificationSettingsCount++
    }
}
