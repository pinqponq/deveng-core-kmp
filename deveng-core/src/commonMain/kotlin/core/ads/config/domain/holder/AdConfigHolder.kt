package core.ads.config.domain.holder

import core.ads.config.domain.model.AdConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory cache of the remote [AdConfig], populated once at app startup.
 * Exposed as a [StateFlow] so ad zones recompose if the config arrives after
 * the screen is shown. Stays null until fetched — there is no hardcoded
 * fallback, so ad zones must not load until a config is present.
 */
class AdConfigHolder {
    private val _adConfig = MutableStateFlow<AdConfig?>(null)
    val adConfig: StateFlow<AdConfig?> = _adConfig.asStateFlow()

    fun update(adConfig: AdConfig) {
        _adConfig.value = adConfig
    }
}
