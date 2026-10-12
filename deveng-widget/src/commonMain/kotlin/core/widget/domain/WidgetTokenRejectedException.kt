package core.widget.domain

/** The server no longer accepts the stored widget token (revoked on logout or unpair); a new one must be issued. */
class WidgetTokenRejectedException(
    rejectionCause: Throwable? = null,
) : IllegalStateException("The widget token was rejected by the server.", rejectionCause)
