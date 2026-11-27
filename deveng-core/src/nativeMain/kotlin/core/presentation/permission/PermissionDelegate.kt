package core.presentation.permission

import core.presentation.permission.PermissionState

internal interface PermissionDelegate {
    suspend fun providePermission()
    suspend fun getPermissionState(): PermissionState
}
