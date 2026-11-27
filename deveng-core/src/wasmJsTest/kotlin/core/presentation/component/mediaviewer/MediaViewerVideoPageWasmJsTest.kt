@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalTestApi::class)

package core.presentation.component.mediaviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement

class MediaViewerVideoPageWasmJsTest {

    @BeforeTest
    fun setUp() {
        makeBodyShadowHostWithoutSlot()
    }

    @Test
    fun mediaViewerVideoPage_whenBodyIsShadowHost_mountsVideoOnDocumentElement() = runComposeUiTest {
        setContent { VideoPageOverRed(areControlsVisible = false) }
        waitForIdle()

        assertSame(document.documentElement, findVideo().parentNode)
    }

    @Test
    fun mediaViewerVideoPage_whenBodyIsShadowHost_rendersVideoWithPageSize() = runComposeUiTest {
        setContent { VideoPageOverRed(areControlsVisible = false) }
        waitForIdle()

        val bounds = findVideo().getBoundingClientRect()

        assertEquals(PAGE_SIZE_DP.toDouble(), bounds.width)
        assertEquals(PAGE_SIZE_DP.toDouble(), bounds.height)
    }

    @Test
    fun mediaViewerVideoPage_withControlsHidden_placesVideoBehindCanvasAndClearsPageArea() = runComposeUiTest {
        setContent { VideoPageOverRed(areControlsVisible = false) }
        waitForIdle()

        val video = findVideo()
        val pageCenterPixel = onRoot().captureToImage().toPixelMap().let { it[it.width / 2, it.height / 2] }

        assertEquals(Z_INDEX_BEHIND_CANVAS, video.style.zIndex)
        assertEquals(POINTER_EVENTS_PASSED_THROUGH, video.style.getPropertyValue(POINTER_EVENTS_PROPERTY))
        assertEquals(0f, pageCenterPixel.alpha)
        assertTrue(isCanvasTransparencyStyleInstalled())
    }

    @Test
    fun mediaViewerVideoPage_withControlsVisible_placesVideoInFrontOfCanvasOverBlackPage() = runComposeUiTest {
        setContent { VideoPageOverRed(areControlsVisible = true) }
        waitForIdle()

        val video = findVideo()
        val pageCenterPixel = onRoot().captureToImage().toPixelMap().let { it[it.width / 2, it.height / 2] }

        assertEquals(Z_INDEX_IN_FRONT_OF_CANVAS, video.style.zIndex)
        assertEquals(POINTER_EVENTS_TAKEN, video.style.getPropertyValue(POINTER_EVENTS_PROPERTY))
        assertEquals(Color.Black, pageCenterPixel)
    }

    @Test
    fun mediaViewerVideoPage_whenLeavingComposition_removesVideoFromPage() = runComposeUiTest {
        var isPageShown by mutableStateOf(true)
        setContent {
            if (isPageShown) {
                VideoPageOverRed(areControlsVisible = false)
            }
        }
        waitForIdle()

        isPageShown = false
        waitForIdle()

        assertNull(document.querySelector(VIDEO_SELECTOR))
    }

    private fun findVideo(): HTMLVideoElement =
        // The page under test adds exactly this element, so a missing one fails the test here.
        document.querySelector(VIDEO_SELECTOR)!!.unsafeCast<HTMLVideoElement>()

    // The red backdrop makes both outcomes visible in a capture: a cleared page area has no colour
    // at all, a black page area hides the red.
    @Composable
    private fun VideoPageOverRed(areControlsVisible: Boolean) {
        Box(modifier = Modifier.size(PAGE_SIZE_DP.dp).background(color = Color.Red)) {
            MediaViewerVideoPage(
                mediaId = MEDIA_ID,
                videoUrl = VIDEO_URL,
                videoContentDescription = VIDEO_CONTENT_DESCRIPTION,
                arePlayerControlsVisible = areControlsVisible,
                isActive = false,
            )
        }
    }

    private companion object {
        const val MEDIA_ID = 7
        const val VIDEO_URL = "collage.mp4"
        const val VIDEO_CONTENT_DESCRIPTION = "collage"
        const val VIDEO_SELECTOR = "video[data-media-id='$MEDIA_ID']"
        const val PAGE_SIZE_DP = 120
        const val Z_INDEX_BEHIND_CANVAS = "-1"
        const val Z_INDEX_IN_FRONT_OF_CANVAS = "1"
        const val POINTER_EVENTS_PROPERTY = "pointer-events"
        const val POINTER_EVENTS_PASSED_THROUGH = "none"
        const val POINTER_EVENTS_TAKEN = "auto"
    }
}

// The same shape ComposeViewport(document.body) leaves behind: an open shadow root without a slot.
private fun makeBodyShadowHostWithoutSlot(): Unit = js(
    """
{
    if (!document.body.shadowRoot) {
        document.body.attachShadow({ mode: 'open' });
    }
}
"""
)

private fun isCanvasTransparencyStyleInstalled(): Boolean = js(
    """
{
    var styles = document.head.querySelectorAll('style');
    for (var index = 0; index < styles.length; index++) {
        if (styles[index].textContent.indexOf('canvas{background:transparent !important;}') >= 0) return true;
    }
    return false;
}
"""
)
