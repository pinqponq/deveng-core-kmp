/*
 * Copyright 2022 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package core.presentation.permission

import core.presentation.permission.DeniedAlwaysException
import core.presentation.permission.DeniedException
import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import platform.Foundation.NSError
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.suspendCoroutine

internal class RemoteNotificationPermissionDelegate : PermissionDelegate {
    override suspend fun providePermission() {
        when (val status: UNAuthorizationStatus = awaitAuthorizationStatus()) {
            in GRANTED_STATUSES -> return

            UNAuthorizationStatusNotDetermined -> requestAuthorization()

            // iOS presents the authorization alert once per installation. Every other status
            // means that single chance is spent, so notifications can only be turned back on
            // from Settings.
            else -> throw DeniedAlwaysException(
                permission = Permission.REMOTE_NOTIFICATION,
                message = "notification authorization status $status cannot be requested again"
            )
        }
    }

    override suspend fun getPermissionState(): PermissionState {
        return when (awaitAuthorizationStatus()) {
            in GRANTED_STATUSES -> PermissionState.Granted
            UNAuthorizationStatusNotDetermined -> PermissionState.NotDetermined
            else -> PermissionState.DeniedAlways
        }
    }

    private suspend fun requestAuthorization() {
        val result: AuthorizationRequestResult = suspendCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(
                    UNAuthorizationOptionSound
                        .or(UNAuthorizationOptionAlert)
                        .or(UNAuthorizationOptionBadge),
                    mainContinuation { isGranted: Boolean, error: NSError? ->
                        val requestResult = when {
                            error != null -> AuthorizationRequestResult.Failed(error)
                            isGranted -> AuthorizationRequestResult.Granted
                            else -> AuthorizationRequestResult.DeniedByUser
                        }
                        continuation.resumeWith(Result.success(requestResult))
                    }
                )
        }

        when (result) {
            AuthorizationRequestResult.Granted -> return

            // The user tapped "Don't Allow", which moves the status to Denied for good.
            AuthorizationRequestResult.DeniedByUser ->
                throw DeniedAlwaysException(Permission.REMOTE_NOTIFICATION)

            // The system could not complete the request and the status stays NotDetermined,
            // so the caller is allowed to ask again.
            is AuthorizationRequestResult.Failed -> throw DeniedException(
                permission = Permission.REMOTE_NOTIFICATION,
                message = "notification authorization request failed: " +
                    result.error.localizedDescription
            )
        }
    }

    private suspend fun awaitAuthorizationStatus(): UNAuthorizationStatus {
        return suspendCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler(
                    mainContinuation { settings: UNNotificationSettings? ->
                        continuation.resumeWith(
                            Result.success(
                                settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined
                            )
                        )
                    }
                )
        }
    }

    private sealed interface AuthorizationRequestResult {
        data object Granted : AuthorizationRequestResult

        data object DeniedByUser : AuthorizationRequestResult

        data class Failed(val error: NSError) : AuthorizationRequestResult
    }

    private companion object {
        val GRANTED_STATUSES = setOf(
            UNAuthorizationStatusAuthorized,
            UNAuthorizationStatusProvisional,
            UNAuthorizationStatusEphemeral
        )
    }
}
