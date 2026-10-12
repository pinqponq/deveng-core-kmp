package core.data.upload

import com.russhwolf.settings.MapSettings
import core.domain.upload.model.BackgroundUploadDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackgroundUploadDescriptorStorageTest {

    private val settings = MapSettings()
    private val storage = BackgroundUploadDescriptorStorage(descriptorSettings = settings)

    @Test
    fun save_thenLoad_returnsSameDescriptor() {
        storage.save(VIDEO_DESCRIPTOR)

        assertEquals(VIDEO_DESCRIPTOR, storage.load(VIDEO_UPLOAD_ID))
    }

    @Test
    fun load_withDescriptorStoredByEarlierRelease_readsEveryFieldAndMediaIdAsRemoteId() {
        settings.putString("bg_upload_descriptor_$VIDEO_UPLOAD_ID", EARLIER_RELEASE_DESCRIPTOR_JSON)

        val descriptor = assertNotNull(storage.load(VIDEO_UPLOAD_ID))

        assertEquals(VIDEO_UPLOAD_ID, descriptor.uploadId)
        assertEquals("widget.mp4", descriptor.mediaFileName)
        assertTrue(descriptor.isVideo)
        assertEquals("video/mp4", descriptor.contentType)
        assertEquals(42, descriptor.remoteId)
    }

    @Test
    fun load_forPhotoDescriptor_isNotVideoAndHasNoRemoteId() {
        storage.save(PHOTO_DESCRIPTOR)

        val descriptor = assertNotNull(storage.load(PHOTO_UPLOAD_ID))

        assertFalse(descriptor.isVideo)
        assertNull(descriptor.remoteId)
    }

    @Test
    fun load_withUnreadableValue_returnsNull() {
        settings.putString("bg_upload_descriptor_$VIDEO_UPLOAD_ID", "not json")

        assertNull(storage.load(VIDEO_UPLOAD_ID))
    }

    @Test
    fun remove_deletesOnlyThatDescriptor() {
        storage.save(VIDEO_DESCRIPTOR)
        storage.save(PHOTO_DESCRIPTOR)

        storage.remove(VIDEO_UPLOAD_ID)

        assertNull(storage.load(VIDEO_UPLOAD_ID))
        assertEquals(PHOTO_DESCRIPTOR, storage.load(PHOTO_UPLOAD_ID))
    }

    @Test
    fun loadAll_returnsStoredDescriptorsAndIgnoresOtherKeys() {
        storage.save(VIDEO_DESCRIPTOR)
        storage.save(PHOTO_DESCRIPTOR)
        settings.putString(UNRELATED_KEY, UNRELATED_VALUE)

        assertEquals(setOf(VIDEO_DESCRIPTOR, PHOTO_DESCRIPTOR), storage.loadAll().toSet())
    }

    private companion object {
        const val VIDEO_UPLOAD_ID = "video-1"
        const val PHOTO_UPLOAD_ID = "photo-1"
        const val UNRELATED_KEY = "TOKEN"
        const val UNRELATED_VALUE = "abc"
        const val EARLIER_RELEASE_DESCRIPTOR_JSON =
            """{"uploadId":"video-1","mediaFileName":"widget.mp4","isVideo":true,"contentType":"video/mp4","mediaId":42}"""
        val VIDEO_DESCRIPTOR = BackgroundUploadDescriptor(
            uploadId = VIDEO_UPLOAD_ID,
            mediaFileName = "widget.mp4",
            isVideo = true,
            contentType = "video/mp4",
            remoteId = 42,
        )
        val PHOTO_DESCRIPTOR = BackgroundUploadDescriptor(
            uploadId = PHOTO_UPLOAD_ID,
            mediaFileName = "widget.jpg",
            isVideo = false,
            contentType = "image/jpeg",
        )
    }
}
