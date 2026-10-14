package core.presentation.baseviewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import core.presentation.baseviewmodel.fake.FakeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class InitEffectTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reenteringScreen_withSameViewModel_initializesOnlyOnce() = runComposeUiTest {
        val viewModel = FakeViewModel(dispatcher = testDispatcher)
        val isInitList = mutableListOf<Boolean>()
        var isScreenShown by mutableStateOf(true)
        setContent {
            if (isScreenShown) {
                InitEffect(viewModel = viewModel) { isInit -> isInitList += isInit }
            }
        }

        waitForIdle()
        isScreenShown = false
        waitForIdle()
        isScreenShown = true
        waitForIdle()

        assertEquals(listOf(true, false), isInitList)
    }
}
