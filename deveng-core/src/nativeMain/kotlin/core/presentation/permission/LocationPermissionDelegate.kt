package core.presentation.permission

import core.presentation.permission.DeniedAlwaysException
import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import platform.CoreLocation.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class LocationPermissionDelegate(
    private val locationManagerDelegate: LocationManagerDelegate,
    private val permission: Permission
) : PermissionDelegate {
    override suspend fun providePermission() {
        return provideLocationPermission(CLLocationManager.authorizationStatus())
    }

    override suspend fun getPermissionState(): PermissionState {
        val status: CLAuthorizationStatus = CLLocationManager.authorizationStatus()
        return when (status) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> PermissionState.Granted

            kCLAuthorizationStatusNotDetermined -> PermissionState.NotDetermined
            kCLAuthorizationStatusDenied -> PermissionState.DeniedAlways
            else -> error("unknown location authorization status $status")
        }
    }

    private suspend fun provideLocationPermission(
        status: CLAuthorizationStatus
    ) {
        when (status) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> return

            kCLAuthorizationStatusNotDetermined -> {
                val newStatus = suspendCoroutine<CLAuthorizationStatus> { continuation ->
                    locationManagerDelegate.requestLocationAccess { continuation.resume(it) }
                }
                provideLocationPermission(newStatus)
            }

            kCLAuthorizationStatusDenied -> throw DeniedAlwaysException(permission)
            else -> error("unknown location authorization status $status")
        }
    }
}
