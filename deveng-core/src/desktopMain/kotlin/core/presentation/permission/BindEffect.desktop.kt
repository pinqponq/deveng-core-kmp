package core.presentation.permission

import androidx.compose.runtime.Composable

// on iOS side we should not do anything to prepare PermissionsController to work
@Suppress("FunctionNaming")
@Composable
actual fun BindEffect(permissionsController: PermissionsController) = Unit