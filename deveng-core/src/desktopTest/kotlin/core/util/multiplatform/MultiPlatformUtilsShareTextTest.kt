package core.util.multiplatform

import kotlinx.coroutines.test.runTest
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MultiPlatformUtilsShareTextTest {

    private val shareClipboard = Clipboard(SHARE_CLIPBOARD_NAME)
    private val multiPlatformUtils = MultiPlatformUtils(shareClipboardProvider = { shareClipboard })

    @Test
    fun shareTextWithResult_withBlankText_reportsNotShown() = runTest {
        val shareTextResult = multiPlatformUtils.shareTextWithResult(text = BLANK_TEXT)

        assertEquals(ShareTextResult(outcome = ShareTextOutcome.NOT_SHOWN), shareTextResult)
    }

    @Test
    fun shareTextWithResult_withText_copiesTextAndReportsCopiedToClipboard() = runTest {
        val shareTextResult = multiPlatformUtils.shareTextWithResult(text = SHARED_TEXT)

        assertEquals(ShareTextResult(outcome = ShareTextOutcome.COPIED_TO_CLIPBOARD), shareTextResult)
        assertNull(shareTextResult.targetApp)
        assertEquals(SHARED_TEXT, shareClipboard.getData(DataFlavor.stringFlavor))
    }

    private companion object {
        const val BLANK_TEXT = "  "
        const val SHARED_TEXT = "Join me on Rindle"
        const val SHARE_CLIPBOARD_NAME = "share-test"
    }
}
