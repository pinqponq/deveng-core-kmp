@file:OptIn(ExperimentalWasmJsInterop::class)

package core.presentation.component.mediaviewer

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement

class NativeVideoSurfaceTest {

    private val surface = NativeVideoSurface(videoElementObserver = null)

    @BeforeTest
    fun setUp() {
        makeBodyShadowHostWithoutSlot()
    }

    @AfterTest
    fun tearDown() {
        surface.detach()
    }

    @Test
    fun attach_whenBodyIsShadowHost_mountsVideoOnDocumentElement() {
        surface.attachHidden()

        val video = findVideo()

        assertSame(document.documentElement, video.parentNode)
    }

    @Test
    fun attach_whenBodyIsShadowHost_rendersVideoAtPageBounds() {
        surface.updateBounds(left = PAGE_LEFT, top = PAGE_TOP, width = PAGE_WIDTH, height = PAGE_HEIGHT)
        surface.attachHidden()

        val bounds = findVideo().getBoundingClientRect()

        assertEquals(PAGE_LEFT, bounds.left)
        assertEquals(PAGE_TOP, bounds.top)
        assertEquals(PAGE_WIDTH, bounds.width)
        assertEquals(PAGE_HEIGHT, bounds.height)
    }

    @Test
    fun attach_withControlsHidden_placesVideoBehindCanvasAndLetsPointerEventsThrough() {
        surface.attachHidden()

        val video = findVideo()

        assertEquals("-1", video.style.zIndex)
        assertEquals("none", video.style.getPropertyValue("pointer-events"))
    }

    @Test
    fun attach_withControlsVisible_placesVideoInFrontOfCanvasAndTakesPointerEvents() {
        surface.attach(mediaId = MEDIA_ID, videoUrl = VIDEO_URL, areControlsVisible = true, shouldStartPlaying = false)

        val video = findVideo()

        assertEquals("1", video.style.zIndex)
        assertEquals("auto", video.style.getPropertyValue("pointer-events"))
    }

    @Test
    fun detach_afterAttach_removesVideoFromPage() {
        surface.attachHidden()

        surface.detach()

        assertNull(document.querySelector(VIDEO_SELECTOR))
    }

    @Test
    fun attach_whenBodyIsShadowHost_makesCanvasTransparentForClearedArea() {
        surface.attachHidden()

        assertTrue(isCanvasTransparencyStyleInstalled())
    }

    private fun NativeVideoSurface.attachHidden() =
        attach(mediaId = MEDIA_ID, videoUrl = VIDEO_URL, areControlsVisible = false, shouldStartPlaying = false)

    private fun findVideo(): HTMLVideoElement =
        // The surface under test adds exactly this element, so a missing one fails the test here.
        document.querySelector(VIDEO_SELECTOR)!!.unsafeCast<HTMLVideoElement>()

    private companion object {
        const val MEDIA_ID = 7
        const val VIDEO_URL = "collage.mp4"
        const val VIDEO_SELECTOR = "video[data-media-id='7']"
        const val PAGE_LEFT = 10.0
        const val PAGE_TOP = 20.0
        const val PAGE_WIDTH = 300.0
        const val PAGE_HEIGHT = 500.0
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
