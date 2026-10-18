package core.push.fake

import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import core.presentation.permission.PermissionsController
import kotlinx.coroutines.yield

class FakePermissionsController : PermissionsController {
    var permissionState = PermissionState.NotDetermined
    var permissionStateFailure: Exception? = null

    /** Thrown by [providePermission]; null grants the permission. */
    var providePermissionFailure: Exception? = null

    /** The state reported after [providePermission] threw [providePermissionFailure]. */
    var permissionStateAfterFailure: PermissionState? = null

    val providedPermissions = mutableListOf<Permission>()
    var openNotificationSettingsCount = 0
    var openNotificationSettingsFailure: Exception? = null

    override suspend fun providePermission(permission: Permission) {
        yield()
        providedPermissions += permission
        val failure = providePermissionFailure
        if (failure == null) {
            permissionState = PermissionState.Granted
            return
        }

        permissionStateAfterFailure?.let { permissionState = it }
        throw failure
    }

    override suspend fun isPermissionGranted(permission: Permission): Boolean {
        return getPermissionState(permission) == PermissionState.Granted
    }

    override suspend fun getPermissionState(permission: Permission): PermissionState {
        yield()
        permissionStateFailure?.let { failure -> throw failure }
        return permissionState
    }

    override fun openAppSettings() = Unit

    override fun openNotificationSettings() {
        openNotificationSettingsFailure?.let { failure -> throw failure }
        openNotificationSettingsCount += 1
    }
}
