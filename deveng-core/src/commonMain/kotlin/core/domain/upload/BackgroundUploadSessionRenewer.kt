package core.domain.upload

/**
 * The app's authentication session as background uploads need it. Implement it on top of the app's
 * auth/session kit (e.g. deveng-networking-kmp's session refresher) so an upload that outlives the
 * access token is retried with a renewed one.
 */
interface BackgroundUploadSessionRenewer {

    /**
     * Renews the access token when it is about to expire, or always when [isForced] is true.
     * Returns false when the session cannot be renewed (signed out, refresh token rejected).
     */
    suspend fun renewSession(isForced: Boolean): Boolean

    /** The current access token, null when there is none. */
    fun fetchAccessToken(): String?
}
