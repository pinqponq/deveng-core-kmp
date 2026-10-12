package core.data.upload

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import core.domain.upload.model.BackgroundUploadDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_PREFIX = "bg_upload_descriptor_"

/**
 * Persists what a background upload needs after the app is killed and relaunched by the OS: the
 * worker or URL session completion reads the descriptor back by upload id.
 */
class BackgroundUploadDescriptorStorage(
    private val descriptorSettings: Settings,
) {
    fun save(descriptor: BackgroundUploadDescriptor) {
        descriptorSettings[descriptorKey(descriptor.uploadId)] = Json.encodeToString(descriptor)
    }

    fun load(uploadId: String): BackgroundUploadDescriptor? {
        val encodedDescriptor = descriptorSettings.getStringOrNull(descriptorKey(uploadId)) ?: return null
        return try {
            Json.decodeFromString<BackgroundUploadDescriptor>(encodedDescriptor)
        } catch (_: Exception) {
            null
        }
    }

    fun remove(uploadId: String) {
        descriptorSettings.remove(descriptorKey(uploadId))
    }

    fun loadAll(): List<BackgroundUploadDescriptor> =
        descriptorSettings.keys
            .filter { key -> key.startsWith(KEY_PREFIX) }
            .mapNotNull { key -> load(key.removePrefix(KEY_PREFIX)) }

    private fun descriptorKey(uploadId: String): String = "$KEY_PREFIX$uploadId"
}
