package core.ads.config.domain.usecase

import core.ads.config.domain.holder.AdConfigHolder
import core.ads.config.domain.model.AdConfig
import core.ads.fake.FakeAdConfigRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FetchAdConfigUseCaseTest {

    private val adConfigRepository = FakeAdConfigRepository()
    private val adConfigHolder = AdConfigHolder()

    @Test
    fun invoke_inReleaseBuild_publishesBackendAdUnits() = runTest {
        adConfigRepository.adConfig = BACKEND_AD_CONFIG

        createUseCase(isTestAdUnitsEnabled = false).invoke()

        assertEquals(BACKEND_AD_CONFIG, adConfigHolder.adConfig.value)
    }

    @Test
    fun invoke_withTestAdUnitsEnabled_publishesGoogleTestAdUnits() = runTest {
        adConfigRepository.adConfig = BACKEND_AD_CONFIG

        createUseCase(isTestAdUnitsEnabled = true).invoke()

        assertEquals(
            AdConfig(
                rewardedAdUnitIdAndroid = "ca-app-pub-3940256099942544/5224354917",
                rewardedAdUnitIdIos = "ca-app-pub-3940256099942544/1712485313",
            ),
            adConfigHolder.adConfig.value,
        )
    }

    @Test
    fun invoke_whenBackendFails_keepsConfigEmptyAndThrows() = runTest {
        adConfigRepository.getAdConfigFailure = IllegalStateException(SERVER_ERROR_MESSAGE)

        assertFailsWith<IllegalStateException> { createUseCase(isTestAdUnitsEnabled = true).invoke() }

        assertNull(adConfigHolder.adConfig.value)
    }

    private fun createUseCase(isTestAdUnitsEnabled: Boolean) = FetchAdConfigUseCase(
        adConfigRepository = adConfigRepository,
        adConfigHolder = adConfigHolder,
        isTestAdUnitsEnabled = isTestAdUnitsEnabled,
    )

    private companion object {
        const val SERVER_ERROR_MESSAGE = "Server error."
        val BACKEND_AD_CONFIG = AdConfig(
            rewardedAdUnitIdAndroid = "ca-app-pub-1111111111111111/1111111111",
            rewardedAdUnitIdIos = "ca-app-pub-1111111111111111/2222222222",
        )
    }
}
