package core.testing

import core.presentation.baseviewmodel.ViewModelInterceptor
import core.presentation.baseviewmodel.ViewModelInterceptors
import core.testing.fake.FakeErrorPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Runs a [core.presentation.baseviewmodel.BaseViewModel] outside the app. [ViewModelInterceptors]
 * is a process-wide registry, so [start] replaces whatever an earlier test left there and [stop]
 * empties it again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTestEnvironment(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) {
    val recordingInterceptor = RecordingInterceptor()
    val errorPrinter = FakeErrorPrinter()

    fun start(additionalInterceptors: List<ViewModelInterceptor> = emptyList()) {
        Dispatchers.setMain(testDispatcher)
        ViewModelInterceptors.unregisterAll()
        ViewModelInterceptors.register(recordingInterceptor)
        additionalInterceptors.forEach(ViewModelInterceptors::register)
    }

    fun stop() {
        ViewModelInterceptors.unregisterAll()
        recordingInterceptor.clear()
        Dispatchers.resetMain()
    }
}
