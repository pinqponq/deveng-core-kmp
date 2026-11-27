package core.util.markdown

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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

    @Test
    fun drawContent_withCodeBlock_showsCodeWithoutFences() = runDesktopComposeUiTest {
        setContent {
            AppTheme(darkTheme = false) {
                MarkdownContentParser(markdownContent = "Before\n```\n$CODE_LINE\n```", textColor = Color.Black)
            }
        }

        onNodeWithText(CODE_LINE).assertIsDisplayed()
        onNodeWithText("Before").assertIsDisplayed()
    }

    @Test
    fun changeContent_afterFirstDraw_showsTheNewContent() = runDesktopComposeUiTest {
        var markdownContent by mutableStateOf(FIRST_CONTENT)
        setContent {
            AppTheme(darkTheme = false) {
                MarkdownContentParser(markdownContent = markdownContent, textColor = Color.Black)
            }
        }

        markdownContent = SECOND_CONTENT
        waitForIdle()

        onNodeWithText(SECOND_CONTENT).assertIsDisplayed()
    }

    private companion object {
        const val CODE_LINE = "val answer = 42"
        const val FIRST_CONTENT = "First reply"
        const val SECOND_CONTENT = "Second reply"
        const val LINK_TEXT = "Open the pull request"
        const val LINK_URL = "https://github.com/pinqponq/deveng-core-kmp/pull/1"
    }
}
