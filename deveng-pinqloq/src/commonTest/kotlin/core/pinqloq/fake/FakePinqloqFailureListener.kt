package core.pinqloq.fake

import core.pinqloq.domain.PinqloqFailureListener
import core.pinqloq.domain.model.PinqloqFailure

class FakePinqloqFailureListener : PinqloqFailureListener {
    val failures = mutableListOf<PinqloqFailure>()

    override fun onFailure(failure: PinqloqFailure) {
        failures += failure
    }
}
