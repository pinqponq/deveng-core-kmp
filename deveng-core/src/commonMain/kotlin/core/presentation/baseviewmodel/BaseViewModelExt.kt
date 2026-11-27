package core.presentation.baseviewmodel

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun BaseViewModel<*, *, *>.launch(
    dispatcher: CoroutineDispatcher,
    notifyProgress: Boolean = true,
    notifyError: Boolean = true,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onError: (exception: Exception) -> Unit = {},
    onCompleted: () -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
): Job = viewModelScope.launch(dispatcher, start) {
    if (notifyProgress) {
        setProgress(showProgress = true)
    }
    try {
        block()
    } catch (exception: CancellationException) {
        // Cancellation is teardown, not failure, so it never reaches setError.
        throw exception
    } catch (exception: Exception) {
        onError(exception)
        if (notifyError) {
            setError(exception = exception)
        } else {
            Logger.e(exception) { exception.message.orEmpty() }
            notifySilentError(exception = exception)
        }
    } finally {
        onCompleted()
        if (notifyProgress) {
            setProgress(showProgress = false)
        }
    }
}
