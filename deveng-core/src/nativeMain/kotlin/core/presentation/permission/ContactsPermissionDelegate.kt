package core.presentation.permission

import core.presentation.permission.DeniedAlwaysException
import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import platform.Contacts.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ContactsPermissionDelegate(
    private val permission: Permission,
    private val contactStore: CNContactStore
) : PermissionDelegate {
    override suspend fun providePermission() {
        return provideLocationPermission(
            CNContactStore.authorizationStatusForEntityType(
                CNEntityType.CNEntityTypeContacts
            )
        )
    }

    override suspend fun getPermissionState(): PermissionState {
        val status: CNAuthorizationStatus =
            CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
        return when (status) {
            CNAuthorizationStatusAuthorized -> PermissionState.Granted

            CNAuthorizationStatusNotDetermined -> PermissionState.NotDetermined
            CNAuthorizationStatusDenied, CNAuthorizationStatusRestricted -> PermissionState.DeniedAlways
            else -> error("unknown contacts $status")
        }
    }

    private suspend fun provideLocationPermission(
        status: CNAuthorizationStatus
    ) {
        when (status) {
            CNAuthorizationStatusAuthorized,
            CNAuthorizationStatusRestricted -> return

            CNAuthorizationStatusNotDetermined -> {
                val newStatus = suspendCoroutine<CNAuthorizationStatus> { continuation ->
                    contactStore.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { flag, error ->
                        continuation.resume(
                            CNContactStore.authorizationStatusForEntityType(
                                CNEntityType.CNEntityTypeContacts
                            )
                        )
                    }
                }
                provideLocationPermission(newStatus)
            }

            CNAuthorizationStatusDenied -> throw DeniedAlwaysException(permission)
            else -> error("unknown location authorization status $status")
        }
    }
}
