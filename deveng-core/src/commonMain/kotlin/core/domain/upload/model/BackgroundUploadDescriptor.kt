package core.domain.upload.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackgroundUploadDescriptor(
    val uploadId: String,
    val mediaFileName: String,
    val isVideo: Boolean,
    val contentType: String,
    /**
     * Id the server reserved for a file that goes straight to storage (presign → transfer → commit).
     * Persisted before the transfer starts so that a completion delivered after the app was killed
     * still knows what to commit. The serial name is the key installed apps already stored.
     */
    @SerialName("mediaId")
    val remoteId: Int? = null,
    /** Set when the file goes to storage in parts; holds which parts are already stored. */
    val partedUpload: PartedUpload? = null,
)
