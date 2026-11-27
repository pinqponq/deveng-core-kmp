package core.presentation.permission

import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.Contacts.CNContactStore
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString
import platform.UIKit.UIApplicationOpenSettingsURLString

class PermissionsControllerIos : PermissionsControllerProtocol {
    private val locationManagerDelegate = LocationManagerDelegate()
    private val contactStore = CNContactStore()

    override suspend fun providePermission(permission: Permission) {
        return getDelegate(permission).providePermission()
    }

    override suspend fun isPermissionGranted(permission: Permission): Boolean {
        return getDelegate(permission).getPermissionState() == PermissionState.Granted
    }

    override suspend fun getPermissionState(permission: Permission): PermissionState {
        return getDelegate(permission).getPermissionState()
    }

    override fun openAppSettings() {
        openSettingsUrl(UIApplicationOpenSettingsURLString)
    }

    override fun openNotificationSettings() {
        if (isNotificationSettingsPageAvailable()) {
            openSettingsUrl(UIApplicationOpenNotificationSettingsURLString)
        } else {
            openAppSettings()
        }
    }

    private fun openSettingsUrl(urlString: String) {
        // A settings URL is always well formed, so a null NSURL here would be a programming error.
        val settingsUrl: NSURL = NSURL.URLWithString(urlString)!!
        UIApplication.sharedApplication.openURL(settingsUrl, mapOf<Any?, Any>(), null)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun isNotificationSettingsPageAvailable(): Boolean {
        return NSProcessInfo.processInfo.operatingSystemVersion.useContents {
            majorVersion >= IOS_VERSION_WITH_NOTIFICATION_SETTINGS_PAGE
        }
    }

    private fun getDelegate(permission: Permission): PermissionDelegate {
        return when (permission) {
            Permission.REMOTE_NOTIFICATION -> RemoteNotificationPermissionDelegate()
            Permission.CAMERA -> AVCapturePermissionDelegate(AVMediaTypeVideo, permission)
            Permission.GALLERY -> GalleryPermissionDelegate()
            Permission.STORAGE, Permission.WRITE_STORAGE -> AlwaysGrantedPermissionDelegate()
            Permission.LOCATION, Permission.COARSE_LOCATION, Permission.BACKGROUND_LOCATION ->
                LocationPermissionDelegate(locationManagerDelegate, permission)

            Permission.RECORD_AUDIO -> AVCapturePermissionDelegate(AVMediaTypeAudio, permission)
            Permission.BLUETOOTH_LE, Permission.BLUETOOTH_SCAN,
            Permission.BLUETOOTH_ADVERTISE, Permission.BLUETOOTH_CONNECT ->
                BluetoothPermissionDelegate(permission)

            Permission.CONTACTS->ContactsPermissionDelegate(permission,contactStore)

            Permission.MOTION -> MotionPermissionDelegate()
        }
    }

    private companion object {
        const val IOS_VERSION_WITH_NOTIFICATION_SETTINGS_PAGE = 16L
    }
}
