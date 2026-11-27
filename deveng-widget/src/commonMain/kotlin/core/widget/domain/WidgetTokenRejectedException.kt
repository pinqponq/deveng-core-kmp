package core.widget.domain

/** The server no longer accepts the stored widget token (it was revoked); a new one must be issued. */
class WidgetTokenRejectedException(
    rejectionCause: Throwable? = null,
) : IllegalStateException("The widget token was rejected by the server.", rejectionCause)
