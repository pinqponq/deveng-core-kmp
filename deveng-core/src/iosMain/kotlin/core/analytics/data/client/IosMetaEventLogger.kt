package core.analytics.data.client

/**
 * Holds the Swift-provided handler that forwards a conversion event to the Facebook SDK. Swift owns
 * FBSDKCoreKit and the app registers the handler from Swift at launch; until then, or
 * when Meta is not configured, [log] is a no-op.
 */
object IosMetaEventLogger {
    private var handler: ((String, Map<String, String>) -> Unit)? = null

    fun setHandler(handler: (String, Map<String, String>) -> Unit) {
        this.handler = handler
    }

    fun log(name: String, params: Map<String, String>) {
        handler?.invoke(name, params)
    }
}
