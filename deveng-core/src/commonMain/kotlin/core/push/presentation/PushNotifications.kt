package core.push.presentation

import co.touchlab.kermit.Logger
import core.presentation.permission.DeniedAlwaysException
import core.presentation.permission.DeniedException
import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import core.presentation.permission.PermissionsController
import core.presentation.permission.RequestCanceledException
import core.push.domain.PushNotificationOptions
import core.push.domain.PushTokenRegistrar
import core.push.domain.PushTokenRegistration
import core.push.domain.PushTokenSource
import core.push.domain.PushTokenUnregistrar
import core.push.domain.SoftAskPolicy
import core.push.domain.model.NotificationPermissionStatus
import core.push.domain.model.NotificationPromptResult
import core.push.domain.store.NotificationPermissionStorage
import core.util.multiplatform.PlatformConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.time.Clock

private const val LOG_TAG = "PushNotifications"

/**
 * The push notification flow of an app: notification permission (status, the app's own request and
 * its cadence, the system prompt) and the device token's registration with the app's backend.
 *
 * Keep one instance per app. Every failure is logged and swallowed, so a caller never shows an error
 * for a notification the user simply declined; coroutine cancellation still propagates.
 *
 * The permission functions take the [PermissionsController] of the calling screen, because on
 * Android a controller works only while it is bound to an activity.
 */
class PushNotifications(
    val options: PushNotificationOptions,
    private val pushTokenSource: PushTokenSource,
    pushTokenRegistrar: PushTokenRegistrar,
    pushTokenUnregistrar: PushTokenUnregistrar,
    platformConfigProvider: () -> PlatformConfig,
    private val notificationPermissionStorage: NotificationPermissionStorage,
    private val isSignedIn: suspend () -> Boolean,
    private val clock: Clock = Clock.System,
) {
    private val logger = Logger.withTag(LOG_TAG)
    private val pushTokenRegistration = PushTokenRegistration(
        pushTokenSource = pushTokenSource,
        pushTokenRegistrar = pushTokenRegistrar,
        pushTokenUnregistrar = pushTokenUnregistrar,
        platformConfigProvider = platformConfigProvider
    )

    /**
     * Reads the current notification permission and translates it into the action the app may take.
     */
    suspend fun resolvePermissionStatus(permissionsController: PermissionsController): NotificationPermissionStatus {
        val permissionState = runCatchingNonCancellation { permissionsController.getPermissionState(Permission.REMOTE_NOTIFICATION) }
            .onFailure { logger.w(it) { "Notification permission state could not be read." } }
            .getOrNull()

        return when (permissionState) {
            PermissionState.Granted -> NotificationPermissionStatus.Granted

            PermissionState.NotDetermined -> NotificationPermissionStatus.Askable

            // Android only: the user declined once and the system still shows its dialog.
            PermissionState.Denied -> NotificationPermissionStatus.Askable

            PermissionState.NotGranted -> if (notificationPermissionStorage.isSystemPromptShown()) {
                NotificationPermissionStatus.BlockedBySystem
            } else {
                NotificationPermissionStatus.Askable
            }

            PermissionState.DeniedAlways -> NotificationPermissionStatus.BlockedBySystem

            // The settings page is the one action that works whatever the real state turns out to be.
            null -> NotificationPermissionStatus.BlockedBySystem
        }
    }

    /**
     * True when the system prompt should open without the app's own request in front of it.
     */
    fun isSystemPromptShownDirectly(notificationPermissionStatus: NotificationPermissionStatus): Boolean {
        val isSystemPromptAvailable = notificationPermissionStatus == NotificationPermissionStatus.Askable
        return !options.isAppMessageShownBeforeSystemPrompt && isSystemPromptAvailable
    }

    /**
     * Registers the device token when notifications are allowed and a user is signed in. Safe to
     * call from every screen and on every app start: an unchanged token is sent only once.
     */
    suspend fun registerTokenIfPermitted(permissionsController: PermissionsController) {
        val status = resolvePermissionStatus(permissionsController)
        if (status != NotificationPermissionStatus.Granted) return

        registerCurrentTokenIfSignedIn()
    }

    /**
     * Decides whether a screen may show the app's own notification request now, and books the ask when
     * it may. Every screen that asks goes through here, so the request keeps one rhythm for the whole
     * app: at most [SoftAskPolicy.maxAskCount] asks, at least [SoftAskPolicy.minimumInterval] apart.
     *
     * Returns the status the request is shown for, or null when no request is due. When the permission
     * is already granted it registers the device token and returns null.
     */
    suspend fun softAsk(permissionsController: PermissionsController): NotificationPermissionStatus? {
        val status = resolvePermissionStatus(permissionsController)

        if (status == NotificationPermissionStatus.Granted) {
            registerCurrentTokenIfSignedIn()
            return null
        }

        if (!isSoftAskDue()) return null

        notificationPermissionStorage.recordAsk(askedAtEpochMilliseconds = clock.now().toEpochMilliseconds())
        return status
    }

    /**
     * Acts on the user's yes: shows the system prompt while the platform still offers it and registers
     * the token on a grant; otherwise opens the system notification settings.
     *
     * Returns null when no prompt was needed because notifications were already allowed.
     */
    suspend fun enableNotifications(permissionsController: PermissionsController): NotificationPromptResult? {
        val status = resolvePermissionStatus(permissionsController)

        if (status == NotificationPermissionStatus.BlockedBySystem) {
            return runCatchingNonCancellation { permissionsController.openNotificationSettings() }
                .onFailure { logger.w(it) { "Notification settings could not be opened." } }
                .map { NotificationPromptResult.SettingsOpened }
                .getOrNull()
        }

        return runCatchingNonCancellation { requestPermissionAndRegisterToken(permissionsController) }
            .onFailure { logger.w(it) { "Notification permission flow failed." } }
            .getOrNull()
    }

    /**
     * Registers a token the platform rotated. Call it from the platform callback that reports a new
     * token; the Android messaging service in core already does.
     */
    suspend fun handleNewToken(token: String) {
        if (!isSignedIn()) return

        runCatchingNonCancellation { pushTokenRegistration.registerToken(token) }
            .onFailure { logger.w(it) { "Push token registration failed for a new token." } }
    }

    /**
     * Registers every token the [PushTokenSource] reports until the calling scope is cancelled. Start
     * it once from the app scope where the platform delivers rotated tokens through the source (iOS).
     */
    suspend fun registerTokenUpdates() {
        pushTokenSource.observeToken()
            .filterNotNull()
            .distinctUntilChanged()
            .collect { token -> handleNewToken(token) }
    }

    /**
     * Removes the device token from the backend on sign-out. Sign-out must finish even when the device
     * cannot be unregistered, so a failure is only logged.
     */
    suspend fun unregisterToken() {
        runCatchingNonCancellation { pushTokenRegistration.unregisterToken() }
            .onFailure { logger.w(it) { "Push token unregister failed during sign-out." } }
    }

    private suspend fun registerCurrentTokenIfSignedIn() {
        if (!isSignedIn()) return

        runCatchingNonCancellation { pushTokenRegistration.registerCurrentToken() }
            .onFailure { logger.w(it) { "Push token registration failed." } }
    }

    private fun isSoftAskDue(): Boolean {
        val softAskPolicy = options.softAskPolicy
        if (notificationPermissionStorage.getAskCount() >= softAskPolicy.maxAskCount) return false
        val lastAskedAt = notificationPermissionStorage.getLastAskedAtEpochMilliseconds() ?: return true

        val elapsedMilliseconds = clock.now().toEpochMilliseconds() - lastAskedAt
        return elapsedMilliseconds >= softAskPolicy.minimumInterval.inWholeMilliseconds
    }

    private suspend fun requestPermissionAndRegisterToken(
        permissionsController: PermissionsController,
    ): NotificationPromptResult? {
        val currentState = permissionsController.getPermissionState(Permission.REMOTE_NOTIFICATION)
        if (currentState == PermissionState.Granted) {
            registerCurrentTokenIfSignedIn()
            return null
        }

        return try {
            // Remembered before the call, because Android reports the same permission state for a
            // dialog that was never shown and one the system refuses to show again.
            notificationPermissionStorage.setSystemPromptShown()
            permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
            registerCurrentTokenIfSignedIn()
            NotificationPromptResult.Granted
        } catch (deniedAlways: DeniedAlwaysException) {
            logger.i { "Notification permission denied permanently: ${deniedAlways.message}" }
            NotificationPromptResult.DeniedPermanently
        } catch (denied: DeniedException) {
            logger.i { "Notification permission denied: ${denied.message}" }
            NotificationPromptResult.Denied
        } catch (canceled: RequestCanceledException) {
            logger.i { "Notification permission request canceled: ${canceled.message}" }
            NotificationPromptResult.Dismissed
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (promptFailure: Exception) {
            // Denying the first iOS prompt arrives as an IllegalStateException, so the answer is read
            // back from the permission state instead of the exception type.
            logger.w(promptFailure) { "Notification permission prompt failed." }
            resolvePromptResultAfterFailure(permissionsController)
        }
    }

    private suspend fun resolvePromptResultAfterFailure(
        permissionsController: PermissionsController,
    ): NotificationPromptResult {
        val permissionState = runCatchingNonCancellation { permissionsController.getPermissionState(Permission.REMOTE_NOTIFICATION) }
            .onFailure { logger.w(it) { "Notification permission state could not be read after the prompt." } }
            .getOrNull()

        return when (permissionState) {
            PermissionState.Granted -> NotificationPromptResult.Granted
            PermissionState.Denied, PermissionState.NotGranted -> NotificationPromptResult.Denied
            PermissionState.DeniedAlways -> NotificationPromptResult.DeniedPermanently
            PermissionState.NotDetermined, null -> NotificationPromptResult.Unknown
        }
    }

    private inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Result.failure(failure)
        }
    }
}
