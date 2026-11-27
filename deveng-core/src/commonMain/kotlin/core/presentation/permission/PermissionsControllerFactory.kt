package core.presentation.permission

import androidx.compose.runtime.Composable

fun interface PermissionsControllerFactory {
    fun createPermissionsController(): PermissionsController
}

@Composable
expect fun rememberPermissionsControllerFactory(): PermissionsControllerFactory
