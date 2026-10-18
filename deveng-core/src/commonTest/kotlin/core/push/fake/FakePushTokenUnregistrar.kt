package core.push.fake

import core.push.domain.PushTokenUnregistrar

class FakePushTokenUnregistrar : PushTokenUnregistrar {
    data class Unregistration(val token: String, val deviceIdentifier: String)

    val unregistrations = mutableListOf<Unregistration>()
    var unregistrationFailure: Exception? = null

    override suspend fun unregister(token: String, deviceIdentifier: String) {
        unregistrations += Unregistration(token = token, deviceIdentifier = deviceIdentifier)
        unregistrationFailure?.let { failure -> throw failure }
    }
}
