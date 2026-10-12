package core.data.upload.fake

import core.domain.upload.BackgroundUploadSessionRenewer

class FakeBackgroundUploadSessionRenewer(
    var accessToken: String? = null,
) : BackgroundUploadSessionRenewer {
    var isRenewable = true
    var renewedAccessToken: String? = null
    val renewRequests = mutableListOf<Boolean>()

    override suspend fun renewSession(isForced: Boolean): Boolean {
        renewRequests += isForced
        if (isForced && isRenewable) {
            renewedAccessToken?.let { token -> accessToken = token }
        }
        return isRenewable
    }

    override fun fetchAccessToken(): String? = accessToken
}
