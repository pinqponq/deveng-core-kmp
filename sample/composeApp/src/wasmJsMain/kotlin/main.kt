import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import global.deveng.core.App
import global.deveng.core.createSampleWebAnalytics
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: return
    createSampleWebAnalytics()
    ComposeViewport(body) {
        App()
    }
}
