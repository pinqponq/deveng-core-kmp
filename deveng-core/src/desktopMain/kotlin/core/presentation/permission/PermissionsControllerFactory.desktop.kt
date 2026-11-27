package core.presentation.permission

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPermissionsControllerFactory(): PermissionsControllerFactory =
    PermissionsControllerFactory {
        // Provide an empty or no-op implementation
        object : PermissionsController {
            // Define any required methods as no-op or default behavior here
            override suspend fun providePermission(permission: Permission) {

            }

            override suspend fun isPermissionGranted(permission: Permission): Boolean {
                return true
            }

            override suspend fun getPermissionState(permission: Permission): PermissionState {
                return PermissionState.Granted
            }

            override fun openAppSettings() {

            }

            override fun openNotificationSettings() {

            }
        }
    }