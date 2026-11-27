package core.presentation.permission

class RequestCanceledException(
    val permission: Permission,
    message: String? = null
) : Exception(message)
