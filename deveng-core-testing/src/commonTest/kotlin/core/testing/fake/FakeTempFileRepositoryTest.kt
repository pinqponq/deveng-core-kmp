package core.testing.fake

import core.domain.temp.TempFileItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeTempFileRepositoryTest {

    private val tempFileRepository = FakeTempFileRepository()

    @Test
    fun saveBytes_twice_storesItemsInOrderWithNormalizedExtension() = runTest {
        val firstItem = tempFileRepository.saveBytes(byteArray = FIRST_BYTES, fileExtension = ".pdf")
        val secondItem = tempFileRepository.saveBytes(byteArray = SECOND_BYTES)

        assertEquals(TempFileItem(id = "temp-file-1", fileName = "temp-file-1.pdf"), firstItem)
        assertEquals(TempFileItem(id = "temp-file-2", fileName = "temp-file-2.jpg"), secondItem)
        assertEquals(listOf(firstItem, secondItem), tempFileRepository.loadAll())
        assertEquals(2, tempFileRepository.getCount())
        assertContentEquals(FIRST_BYTES, tempFileRepository.loadBytes(firstItem))
    }

    @Test
    fun saveBytes_withFailure_throwsAndStoresNothing() = runTest {
        tempFileRepository.saveBytesFailure = IllegalStateException("disk full")

        assertFailsWith<IllegalStateException> { tempFileRepository.saveBytes(byteArray = FIRST_BYTES) }

        assertEquals(0, tempFileRepository.getCount())
    }

    @Test
    fun updateBytes_withStoredItem_replacesBytes() = runTest {
        val item = tempFileRepository.saveBytes(byteArray = FIRST_BYTES)

        val isUpdated = tempFileRepository.updateBytes(itemId = item.id, byteArray = SECOND_BYTES)

        assertTrue(isUpdated)
        assertContentEquals(SECOND_BYTES, tempFileRepository.loadBytes(item))
    }

    @Test
    fun updateBytes_withMissingItem_returnsFalseAndStoresNothing() = runTest {
        val isUpdated = tempFileRepository.updateBytes(itemId = MISSING_ITEM_ID, byteArray = SECOND_BYTES)

        assertFalse(isUpdated)
        assertNull(tempFileRepository.loadBytes(TempFileItem(id = MISSING_ITEM_ID, fileName = MISSING_ITEM_ID)))
    }

    @Test
    fun delete_withStoredItem_removesItAndItsBytes() = runTest {
        val firstItem = tempFileRepository.saveBytes(byteArray = FIRST_BYTES)
        val secondItem = tempFileRepository.saveBytes(byteArray = SECOND_BYTES)

        tempFileRepository.delete(itemId = firstItem.id)

        assertEquals(listOf(secondItem), tempFileRepository.loadAll())
        assertNull(tempFileRepository.loadBytes(firstItem))
    }

    private companion object {
        const val MISSING_ITEM_ID = "temp-file-missing"
        val FIRST_BYTES = byteArrayOf(1, 2, 3)
        val SECOND_BYTES = byteArrayOf(4, 5)
    }
}
