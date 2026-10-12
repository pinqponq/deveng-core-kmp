package core.testing

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
abstract class ScreenRobot<Event>(protected val composeUiTest: ComposeUiTest) {

    private val sentEventList = mutableListOf<Event>()

    val sentEvents: List<Event> get() = sentEventList

    protected fun recordEvent(event: Event) {
        sentEventList += event
    }

    protected fun resolveString(resource: StringResource): String = runBlocking { getString(resource) }
}
