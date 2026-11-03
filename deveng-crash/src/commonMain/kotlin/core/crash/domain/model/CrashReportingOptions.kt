package core.crash.domain.model

private const val DEFAULT_INSTALL_ID_KEY = "install_id"

/**
 * @param installIdKey custom key that carries the installation id (`PlatformConfig.uuid`) on every identify.
 * @param isEnabled false makes every call a no-op on every platform.
 */
data class CrashReportingOptions(
    val installIdKey: String = DEFAULT_INSTALL_ID_KEY,
    val isEnabled: Boolean = true,
) {
    init {
        require(installIdKey.isNotBlank()) { "installIdKey must not be blank" }
    }
}
