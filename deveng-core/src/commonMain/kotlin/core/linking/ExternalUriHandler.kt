package core.linking

object ExternalUriHandler {
    private var pendingUri: String? = null

    internal var listener: ((uri: String) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                pendingUri?.let(value)
                pendingUri = null
            }
        }

    fun onNewUri(uri: String) {
        pendingUri = uri
        listener?.let {
            it.invoke(uri)
            pendingUri = null
        }
    }
}
