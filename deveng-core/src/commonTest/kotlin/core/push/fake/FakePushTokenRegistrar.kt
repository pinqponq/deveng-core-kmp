package core.push.fake

import core.push.domain.PushTokenRegistrar
import core.util.multiplatform.Platform
import kotlinx.coroutines.CompletableDeferred

class FakePushTokenRegistrar : PushTokenRegistrar {
    data class Registration(val token: String, val platform: Platform, val deviceIdentifier: String)

    val registrations = mutableListOf<Registration>()
    var registrationFailure: Exception? = null

    /** When set, every call waits for it, so concurrent calls overlap inside the registrar. */
    var registrationGate: CompletableDeferred<Unit>? = null

    override suspend fun register(token: String, platform: Platform, deviceIdentifier: String) {
        registrationGate?.await()
        registrationFailure?.let { failure -> throw failure }
        registrations += Registration(token = token, platform = platform, deviceIdentifier = deviceIdentifier)
    }
}
