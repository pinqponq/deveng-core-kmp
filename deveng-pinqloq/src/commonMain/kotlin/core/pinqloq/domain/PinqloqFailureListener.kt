package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqFailure
import core.util.CustomLogger

fun interface PinqloqFailureListener {
    fun onFailure(failure: PinqloqFailure)

    companion object {
        val LogToConsole = PinqloqFailureListener { failure ->
            CustomLogger.log(
                "Pinqloq ${failure.reason}: dropped ${failure.droppedLogCount} log(s)" +
                    (failure.cause?.let { cause -> ", cause: $cause" } ?: "")
            )
        }
    }
}
