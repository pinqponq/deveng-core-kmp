package core.testing.fake

import core.domain.temp.TempFileItem
import core.domain.temp.TempFileRepository

class FakeTempFileRepository : TempFileRepository {

    private val storedBytesById = linkedMapOf<String, ByteArray>()
    private val storedItemList = mutableListOf<TempFileItem>()
    private var savedItemCount = 0

    var saveBytesFailure: Exception? = null

    override suspend fun saveBytes(byteArray: ByteArray, fileExtension: String): TempFileItem {
        saveBytesFailure?.let { failure -> throw failure }
        savedItemCount++
        val itemId = "$ITEM_ID_PREFIX$savedItemCount"
        val item = TempFileItem(id = itemId, fileName = "$itemId.${fileExtension.removePrefix(".")}")
        storedItemList += item
        storedBytesById[itemId] = byteArray
        return item
    }

    override suspend fun loadAll(): List<TempFileItem> = storedItemList.toList()

    override suspend fun getCount(): Int = storedItemList.size

    override suspend fun loadBytes(item: TempFileItem): ByteArray? = storedBytesById[item.id]

    override suspend fun updateBytes(itemId: String, byteArray: ByteArray): Boolean {
        if (itemId !in storedBytesById) return false

        storedBytesById[itemId] = byteArray
        return true
    }

    override suspend fun delete(itemId: String) {
        storedItemList.removeAll { item -> item.id == itemId }
        storedBytesById.remove(itemId)
    }

    private companion object {
        const val ITEM_ID_PREFIX = "temp-file-"
    }
}
