package core.util.media

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteMediaSaveHttpStatusTest {

    @Test
    fun isSuccessHttpStatus_for2xxBounds_returnsTrue() {
        assertTrue(isSuccessHttpStatus(200))
        assertTrue(isSuccessHttpStatus(299))
    }

    @Test
    fun isSuccessHttpStatus_forExpiredLinkDeletedItemAndRedirect_returnsFalse() {
        assertFalse(isSuccessHttpStatus(403))
        assertFalse(isSuccessHttpStatus(404))
        assertFalse(isSuccessHttpStatus(300))
        assertFalse(isSuccessHttpStatus(199))
    }
}
