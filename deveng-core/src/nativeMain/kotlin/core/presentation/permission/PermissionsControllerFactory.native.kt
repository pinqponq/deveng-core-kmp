package core.presentation.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPermissionsControllerFactory(): PermissionsControllerFactory {
    return remember {
        PermissionsControllerFactory {
            PermissionsControllerIos()
        }
    }
}