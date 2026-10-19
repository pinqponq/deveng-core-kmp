package core.util.markdown

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MarkdownContentParserTest {

    @Test
    fun clickLink_withMarkdownLink_opensItsUrl() = runDesktopComposeUiTest {
        val openedUrls = mutableListOf<String>()
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                openedUrls += uri
            }
        }
        setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                AppTheme(darkTheme = false) {
                    MarkdownContentParser(markdownContent = "[$LINK_TEXT]($LINK_URL)", textColor = Color.Black)
                }
            }
        }

        onNodeWithText(LINK_TEXT).performClick()

        assertEquals(listOf(LINK_URL), openedUrls)
    }

    private companion object {
        const val LINK_TEXT = "Open the pull request"
        const val LINK_URL = "https://github.com/pinqponq/deveng-core-kmp/pull/1"
    }
}
