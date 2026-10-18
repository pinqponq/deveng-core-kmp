package core.push.presentation

import core.presentation.permission.DeniedAlwaysException
import core.presentation.permission.DeniedException
import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import core.presentation.permission.PermissionsController
import core.presentation.permission.RequestCanceledException
import core.push.domain.PushNotificationOptions
import core.push.domain.PushTokenSource
import core.push.domain.SoftAskPolicy
import core.push.domain.model.NotificationPermissionStatus
import core.push.domain.model.NotificationPromptResult
import core.push.fake.FakeClock
import core.push.fake.FakeNotificationPermissionStorage
import core.push.fake.FakePermissionsController
import core.push.fake.FakePushTokenRegistrar
import core.push.fake.FakePushTokenSource
import core.push.fake.FakePushTokenUnregistrar
import core.util.multiplatform.Platform
import core.util.multiplatform.PlatformConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PushNotificationsTest {

    private val permissionsController = FakePermissionsController()
    private val notificationPermissionStorage = FakeNotificationPermissionStorage()
    private val pushTokenSource = FakePushTokenSource(initialToken = FIRST_TOKEN)
    private val pushTokenRegistrar = FakePushTokenRegistrar()
    private val pushTokenUnregistrar = FakePushTokenUnregistrar()
    private val clock = FakeClock()
    private var isSignedIn = true

    @Test
    fun resolvePermissionStatus_granted_returnsGranted() = runTest {
        permissionsController.permissionState = PermissionState.Granted

        assertEquals(NotificationPermissionStatus.Granted, createPushNotifications().resolvePermissionStatus(permissionsController))
    }

    @Test
    fun resolvePermissionStatus_notDetermined_returnsAskable() = runTest {
        permissionsController.permissionState = PermissionState.NotDetermined

        assertEquals(NotificationPermissionStatus.Askable, createPushNotifications().resolvePermissionStatus(permissionsController))
    }

    @Test
    fun resolvePermissionStatus_deniedOnce_returnsAskable() = runTest {
        permissionsController.permissionState = PermissionState.Denied

        assertEquals(NotificationPermissionStatus.Askable, createPushNotifications().resolvePermissionStatus(permissionsController))
    }

    @Test
    fun resolvePermissionStatus_notGrantedBeforeSystemPrompt_returnsAskable() = runTest {
        permissionsController.permissionState = PermissionState.NotGranted

        assertEquals(NotificationPermissionStatus.Askable, createPushNotifications().resolvePermissionStatus(permissionsController))
    }

    @Test
    fun resolvePermissionStatus_notGrantedAfterSystemPrompt_returnsBlockedBySystem() = runTest {
        permissionsController.permissionState = PermissionState.NotGranted
        notificationPermissionStorage.storedIsSystemPromptShown = true

        assertEquals(NotificationPermissionStatus.BlockedBySystem, createPushNotifications().resolvePermissionStatus(permissionsController))
    }

    @Test
    fun resolvePermissionStatus_deniedAlways_returnsBlockedBySystem() = runTest {
        permissionsController.permissionState = PermissionState.DeniedAlways

        assertEquals(NotificationPermissionStatus.BlockedBySystem, createPushNotifications().resolvePermissionStatus(permissionsController))
    }

    @Test
    fun resolvePermissionStatus_stateReadFails_returnsBlockedBySystem() = runTest {
        permissionsController.permissionStateFailure = IllegalStateException(UNBOUND_CONTROLLER_MESSAGE)

        assertEquals(NotificationPermissionStatus.BlockedBySystem, createPushNotifications().resolvePermissionStatus(permissionsController))
    }

    @Test
    fun isSystemPromptShownDirectly_withoutAppMessageAndAskable_returnsTrue() {
        val pushNotifications = createPushNotifications(isAppMessageShownBeforeSystemPrompt = false)

        assertTrue(pushNotifications.isSystemPromptShownDirectly(NotificationPermissionStatus.Askable))
        assertFalse(pushNotifications.isSystemPromptShownDirectly(NotificationPermissionStatus.BlockedBySystem))
    }

    @Test
    fun isSystemPromptShownDirectly_withAppMessage_returnsFalse() {
        val pushNotifications = createPushNotifications(isAppMessageShownBeforeSystemPrompt = true)

        assertFalse(pushNotifications.isSystemPromptShownDirectly(NotificationPermissionStatus.Askable))
    }

    @Test
    fun softAsk_firstTime_returnsStatusAndRecordsAsk() = runTest {
        val status = createPushNotifications().softAsk(permissionsController)

        assertEquals(NotificationPermissionStatus.Askable, status)
        assertEquals(1, notificationPermissionStorage.storedAskCount)
        assertEquals(clock.currentInstant.toEpochMilliseconds(), notificationPermissionStorage.storedLastAskedAtEpochMilliseconds)
    }

    @Test
    fun softAsk_beforeMinimumInterval_returnsNullWithoutRecordingAsk() = runTest {
        val pushNotifications = createPushNotifications()
        pushNotifications.softAsk(permissionsController)

        clock.advanceBy(SOFT_ASK_INTERVAL - 1.milliseconds)

        assertNull(pushNotifications.softAsk(permissionsController))
        assertEquals(1, notificationPermissionStorage.storedAskCount)
    }

    @Test
    fun softAsk_afterMinimumInterval_returnsStatusAgain() = runTest {
        val pushNotifications = createPushNotifications()
        pushNotifications.softAsk(permissionsController)

        clock.advanceBy(SOFT_ASK_INTERVAL)

        assertEquals(NotificationPermissionStatus.Askable, pushNotifications.softAsk(permissionsController))
        assertEquals(2, notificationPermissionStorage.storedAskCount)
    }

    @Test
    fun softAsk_atMaxAskCount_returnsNull() = runTest {
        notificationPermissionStorage.storedAskCount = MAX_ASK_COUNT

        assertNull(createPushNotifications().softAsk(permissionsController))
        assertEquals(MAX_ASK_COUNT, notificationPermissionStorage.storedAskCount)
    }

    @Test
    fun softAsk_belowMaxAskCount_returnsStatus() = runTest {
        notificationPermissionStorage.storedAskCount = MAX_ASK_COUNT - 1

        assertEquals(NotificationPermissionStatus.Askable, createPushNotifications().softAsk(permissionsController))
    }

    @Test
    fun softAsk_blockedBySystem_returnsBlockedBySystem() = runTest {
        permissionsController.permissionState = PermissionState.DeniedAlways

        assertEquals(NotificationPermissionStatus.BlockedBySystem, createPushNotifications().softAsk(permissionsController))
    }

    @Test
    fun softAsk_granted_registersTokenAndReturnsNullWithoutRecordingAsk() = runTest {
        permissionsController.permissionState = PermissionState.Granted

        assertNull(createPushNotifications().softAsk(permissionsController))
        assertEquals(listOf(FIRST_TOKEN), pushTokenRegistrar.registrations.map { it.token })
        assertEquals(0, notificationPermissionStorage.storedAskCount)
    }

    @Test
    fun registerTokenIfPermitted_grantedAndSignedIn_registersTokenWithPlatformAndDevice() = runTest {
        permissionsController.permissionState = PermissionState.Granted

        createPushNotifications().registerTokenIfPermitted(permissionsController)

        assertEquals(
            listOf(FakePushTokenRegistrar.Registration(token = FIRST_TOKEN, platform = Platform.ANDROID, deviceIdentifier = DEVICE_IDENTIFIER)),
            pushTokenRegistrar.registrations
        )
    }

    @Test
    fun registerTokenIfPermitted_notSignedIn_skipsRegistration() = runTest {
        permissionsController.permissionState = PermissionState.Granted
        isSignedIn = false

        createPushNotifications().registerTokenIfPermitted(permissionsController)

        assertTrue(pushTokenRegistrar.registrations.isEmpty())
    }

    @Test
    fun registerTokenIfPermitted_notGranted_skipsRegistration() = runTest {
        permissionsController.permissionState = PermissionState.NotDetermined

        createPushNotifications().registerTokenIfPermitted(permissionsController)

        assertTrue(pushTokenRegistrar.registrations.isEmpty())
    }

    @Test
    fun registerTokenIfPermitted_sourceWithoutToken_skipsRegistration() = runTest {
        permissionsController.permissionState = PermissionState.Granted

        createPushNotifications(pushTokenSource = TokenlessPushTokenSource).registerTokenIfPermitted(permissionsController)

        assertTrue(pushTokenRegistrar.registrations.isEmpty())
    }

    @Test
    fun registerTokenIfPermitted_tokenNeverArrives_givesUpWithoutRegistering() = runTest {
        permissionsController.permissionState = PermissionState.Granted

        createPushNotifications(pushTokenSource = FakePushTokenSource(initialToken = null)).registerTokenIfPermitted(permissionsController)

        assertTrue(pushTokenRegistrar.registrations.isEmpty())
    }

    @Test
    fun registerTokenIfPermitted_calledTwiceWithSameToken_registersOnce() = runTest {
        permissionsController.permissionState = PermissionState.Granted
        val pushNotifications = createPushNotifications()

        pushNotifications.registerTokenIfPermitted(permissionsController)
        pushNotifications.registerTokenIfPermitted(permissionsController)

        assertEquals(1, pushTokenRegistrar.registrations.size)
    }

    @Test
    fun registerTokenIfPermitted_concurrentCalls_registersOnce() = runTest {
        permissionsController.permissionState = PermissionState.Granted
        val registrationGate = CompletableDeferred<Unit>()
        pushTokenRegistrar.registrationGate = registrationGate
        val pushNotifications = createPushNotifications()

        val firstCall = launch { pushNotifications.registerTokenIfPermitted(permissionsController) }
        val secondCall = launch { pushNotifications.registerTokenIfPermitted(permissionsController) }
        runCurrent()
        registrationGate.complete(Unit)
        firstCall.join()
        secondCall.join()

        assertEquals(1, pushTokenRegistrar.registrations.size)
    }

    @Test
    fun registerTokenIfPermitted_registrarFails_swallowsErrorAndRetriesNextTime() = runTest {
        permissionsController.permissionState = PermissionState.Granted
        val pushNotifications = createPushNotifications()
        pushTokenRegistrar.registrationFailure = IllegalStateException(BACKEND_FAILURE_MESSAGE)

        pushNotifications.registerTokenIfPermitted(permissionsController)
        pushTokenRegistrar.registrationFailure = null
        pushNotifications.registerTokenIfPermitted(permissionsController)

        assertEquals(listOf(FIRST_TOKEN), pushTokenRegistrar.registrations.map { it.token })
    }

    @Test
    fun handleNewToken_rotatedToken_registersNewToken() = runTest {
        val pushNotifications = createPushNotifications()

        pushNotifications.handleNewToken(FIRST_TOKEN)
        pushNotifications.handleNewToken(FIRST_TOKEN)
        pushNotifications.handleNewToken(ROTATED_TOKEN)

        assertEquals(listOf(FIRST_TOKEN, ROTATED_TOKEN), pushTokenRegistrar.registrations.map { it.token })
    }

    @Test
    fun handleNewToken_notSignedIn_skipsRegistration() = runTest {
        isSignedIn = false

        createPushNotifications().handleNewToken(ROTATED_TOKEN)

        assertTrue(pushTokenRegistrar.registrations.isEmpty())
    }

    @Test
    fun handleNewToken_registrarFails_swallowsError() = runTest {
        pushTokenRegistrar.registrationFailure = IllegalStateException(BACKEND_FAILURE_MESSAGE)

        createPushNotifications().handleNewToken(ROTATED_TOKEN)

        assertTrue(pushTokenRegistrar.registrations.isEmpty())
    }

    @Test
    fun registerTokenUpdates_sourceRotatesToken_registersEachNewToken() = runTest {
        val pushNotifications = createPushNotifications()

        backgroundScope.launch { pushNotifications.registerTokenUpdates() }
        runCurrent()
        pushTokenSource.token.value = ROTATED_TOKEN
        runCurrent()

        assertEquals(listOf(FIRST_TOKEN, ROTATED_TOKEN), pushTokenRegistrar.registrations.map { it.token })
    }

    @Test
    fun unregisterToken_afterRegistration_unregistersTokenAndNextSignInRegistersAgain() = runTest {
        val pushNotifications = createPushNotifications()
        pushNotifications.handleNewToken(FIRST_TOKEN)

        pushNotifications.unregisterToken()
        pushNotifications.handleNewToken(FIRST_TOKEN)

        assertEquals(
            listOf(FakePushTokenUnregistrar.Unregistration(token = FIRST_TOKEN, deviceIdentifier = DEVICE_IDENTIFIER)),
            pushTokenUnregistrar.unregistrations
        )
        assertEquals(2, pushTokenRegistrar.registrations.size)
    }

    @Test
    fun unregisterToken_unregistrarFails_clearsCacheAndSwallowsError() = runTest {
        val pushNotifications = createPushNotifications()
        pushNotifications.handleNewToken(ROTATED_TOKEN)
        pushTokenUnregistrar.unregistrationFailure = IllegalStateException(BACKEND_FAILURE_MESSAGE)

        pushNotifications.unregisterToken()
        pushNotifications.handleNewToken(ROTATED_TOKEN)

        assertEquals(listOf(ROTATED_TOKEN), pushTokenUnregistrar.unregistrations.map { it.token })
        assertEquals(listOf(ROTATED_TOKEN, ROTATED_TOKEN), pushTokenRegistrar.registrations.map { it.token })
    }

    @Test
    fun unregisterToken_withoutRegistration_unregistersCurrentSourceToken() = runTest {
        createPushNotifications().unregisterToken()

        assertEquals(listOf(FIRST_TOKEN), pushTokenUnregistrar.unregistrations.map { it.token })
    }

    @Test
    fun unregisterToken_noTokenKnown_skipsUnregister() = runTest {
        createPushNotifications(pushTokenSource = FakePushTokenSource(initialToken = null)).unregisterToken()

        assertTrue(pushTokenUnregistrar.unregistrations.isEmpty())
    }

    @Test
    fun enableNotifications_blockedBySystem_opensNotificationSettings() = runTest {
        permissionsController.permissionState = PermissionState.DeniedAlways

        val result = createPushNotifications().enableNotifications(permissionsController)

        assertEquals(NotificationPromptResult.SettingsOpened, result)
        assertEquals(1, permissionsController.openNotificationSettingsCount)
        assertTrue(permissionsController.providedPermissions.isEmpty())
    }

    @Test
    fun enableNotifications_settingsCannotOpen_returnsNull() = runTest {
        permissionsController.permissionState = PermissionState.DeniedAlways
        permissionsController.openNotificationSettingsFailure = IllegalStateException(UNBOUND_CONTROLLER_MESSAGE)

        assertNull(createPushNotifications().enableNotifications(permissionsController))
    }

    @Test
    fun enableNotifications_askableAndGranted_marksPromptShownAndRegistersToken() = runTest {
        val result = createPushNotifications().enableNotifications(permissionsController)

        assertEquals(NotificationPromptResult.Granted, result)
        assertEquals(listOf(Permission.REMOTE_NOTIFICATION), permissionsController.providedPermissions)
        assertTrue(notificationPermissionStorage.storedIsSystemPromptShown)
        assertEquals(listOf(FIRST_TOKEN), pushTokenRegistrar.registrations.map { it.token })
        assertEquals(0, permissionsController.openNotificationSettingsCount)
    }

    @Test
    fun enableNotifications_alreadyGranted_registersTokenWithoutPrompt() = runTest {
        permissionsController.permissionState = PermissionState.Granted

        val result = createPushNotifications().enableNotifications(permissionsController)

        assertNull(result)
        assertTrue(permissionsController.providedPermissions.isEmpty())
        assertEquals(listOf(FIRST_TOKEN), pushTokenRegistrar.registrations.map { it.token })
    }

    @Test
    fun enableNotifications_userDenies_returnsDeniedWithoutRegistering() = runTest {
        permissionsController.providePermissionFailure = DeniedException(Permission.REMOTE_NOTIFICATION)

        assertEquals(NotificationPromptResult.Denied, createPushNotifications().enableNotifications(permissionsController))
        assertTrue(notificationPermissionStorage.storedIsSystemPromptShown)
        assertTrue(pushTokenRegistrar.registrations.isEmpty())
    }

    @Test
    fun enableNotifications_userDeniesForGood_returnsDeniedPermanently() = runTest {
        permissionsController.providePermissionFailure = DeniedAlwaysException(Permission.REMOTE_NOTIFICATION)

        assertEquals(NotificationPromptResult.DeniedPermanently, createPushNotifications().enableNotifications(permissionsController))
    }

    @Test
    fun enableNotifications_userCancels_returnsDismissed() = runTest {
        permissionsController.providePermissionFailure = RequestCanceledException(Permission.REMOTE_NOTIFICATION)

        assertEquals(NotificationPromptResult.Dismissed, createPushNotifications().enableNotifications(permissionsController))
    }

    @Test
    fun enableNotifications_promptFailsAndStateGranted_returnsGranted() = runTest {
        assertPromptFailureResult(stateAfterFailure = PermissionState.Granted, expectedResult = NotificationPromptResult.Granted)
    }

    @Test
    fun enableNotifications_promptFailsAndStateNotGranted_returnsDenied() = runTest {
        assertPromptFailureResult(stateAfterFailure = PermissionState.NotGranted, expectedResult = NotificationPromptResult.Denied)
    }

    @Test
    fun enableNotifications_promptFailsAndStateDenied_returnsDenied() = runTest {
        assertPromptFailureResult(stateAfterFailure = PermissionState.Denied, expectedResult = NotificationPromptResult.Denied)
    }

    @Test
    fun enableNotifications_promptFailsAndStateDeniedAlways_returnsDeniedPermanently() = runTest {
        assertPromptFailureResult(stateAfterFailure = PermissionState.DeniedAlways, expectedResult = NotificationPromptResult.DeniedPermanently)
    }

    @Test
    fun enableNotifications_promptFailsAndStateUnknown_returnsUnknown() = runTest {
        assertPromptFailureResult(stateAfterFailure = PermissionState.NotDetermined, expectedResult = NotificationPromptResult.Unknown)
    }

    @Test
    fun enableNotifications_promptFailsAndStateUnreadable_returnsUnknown() = runTest {
        val pushNotifications = createPushNotifications()
        permissionsController.providePermissionFailure = IllegalStateException(IOS_FIRST_DENIAL_MESSAGE)
        val unreadableAfterPrompt = object : PermissionsController by permissionsController {
            private var stateReadCount = 0

            override suspend fun getPermissionState(permission: Permission): PermissionState {
                stateReadCount += 1
                if (stateReadCount > 2) throw IllegalStateException(UNBOUND_CONTROLLER_MESSAGE)
                return permissionsController.getPermissionState(permission)
            }
        }

        assertEquals(NotificationPromptResult.Unknown, pushNotifications.enableNotifications(unreadableAfterPrompt))
    }

    @Test
    fun enableNotifications_stateReadFailsInsidePrompt_returnsNull() = runTest {
        val pushNotifications = createPushNotifications()
        val failingAfterStatus = object : PermissionsController by permissionsController {
            private var stateReadCount = 0

            override suspend fun getPermissionState(permission: Permission): PermissionState {
                stateReadCount += 1
                if (stateReadCount > 1) throw IllegalStateException(UNBOUND_CONTROLLER_MESSAGE)
                return permissionsController.getPermissionState(permission)
            }
        }

        assertNull(pushNotifications.enableNotifications(failingAfterStatus))
        assertTrue(permissionsController.providedPermissions.isEmpty())
    }

    private suspend fun assertPromptFailureResult(stateAfterFailure: PermissionState, expectedResult: NotificationPromptResult) {
        permissionsController.permissionState = PermissionState.NotDetermined
        permissionsController.providePermissionFailure = IllegalStateException(IOS_FIRST_DENIAL_MESSAGE)
        permissionsController.permissionStateAfterFailure = stateAfterFailure

        assertEquals(expectedResult, createPushNotifications().enableNotifications(permissionsController))
    }

    private fun createPushNotifications(
        isAppMessageShownBeforeSystemPrompt: Boolean = true,
        pushTokenSource: PushTokenSource = this.pushTokenSource,
    ) = PushNotifications(
        options = PushNotificationOptions(
            androidChannelId = CHANNEL_ID,
            androidChannelName = CHANNEL_NAME,
            softAskPolicy = SoftAskPolicy(maxAskCount = MAX_ASK_COUNT, minimumInterval = SOFT_ASK_INTERVAL),
            isAppMessageShownBeforeSystemPrompt = isAppMessageShownBeforeSystemPrompt,
        ),
        pushTokenSource = pushTokenSource,
        pushTokenRegistrar = pushTokenRegistrar,
        pushTokenUnregistrar = pushTokenUnregistrar,
        platformConfigProvider = { PLATFORM_CONFIG },
        notificationPermissionStorage = notificationPermissionStorage,
        isSignedIn = { isSignedIn },
        clock = clock,
    )

    private object TokenlessPushTokenSource : PushTokenSource {
        override fun observeToken(): Flow<String?> = emptyFlow()
    }

    private companion object {
        const val FIRST_TOKEN = "first-token"
        const val ROTATED_TOKEN = "rotated-token"
        const val DEVICE_IDENTIFIER = "device-identifier"
        const val CHANNEL_ID = "test_channel"
        const val CHANNEL_NAME = "Test channel"
        const val MAX_ASK_COUNT = 3
        const val BACKEND_FAILURE_MESSAGE = "backend unavailable"
        const val UNBOUND_CONTROLLER_MESSAGE = "controller is not bound"
        const val IOS_FIRST_DENIAL_MESSAGE = "notifications not allowed"
        val SOFT_ASK_INTERVAL = 2.days
        val PLATFORM_CONFIG = PlatformConfig(
            platform = Platform.ANDROID,
            systemLanguage = "en",
            uuid = DEVICE_IDENTIFIER,
            deviceName = "test device",
            packageVersionName = "1.0.0",
        )
    }
}
