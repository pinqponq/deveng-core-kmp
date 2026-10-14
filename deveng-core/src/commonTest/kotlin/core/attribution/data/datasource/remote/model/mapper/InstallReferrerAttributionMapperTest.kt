package core.attribution.data.datasource.remote.model.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstallReferrerAttributionMapperTest {

    private val installReferrerAttributionMapper = InstallReferrerAttributionMapper()

    @Test
    fun mapToDomain_withGoogleAdsClickReferrer_isAttributedWithDecodedCampaignFields() {
        val attribution = installReferrerAttributionMapper.mapToDomain(installReferrer = GOOGLE_ADS_REFERRER)

        assertTrue(attribution.isAttributed)
        assertEquals(GOOGLE_CLICK_ID, attribution.googleClickId)
        assertEquals("google", attribution.campaignSource)
        assertEquals("cpc", attribution.campaignMedium)
        assertEquals("korea ad first", attribution.campaignName)
        assertEquals("couple app", attribution.campaignTerm)
        assertEquals("banner_1", attribution.campaignContent)
    }

    @Test
    fun mapToDomain_withGoogleClickIdOnly_isAttributed() {
        val attribution = installReferrerAttributionMapper.mapToDomain(installReferrer = "gclid=$GOOGLE_CLICK_ID")

        assertTrue(attribution.isAttributed)
        assertEquals(GOOGLE_CLICK_ID, attribution.googleClickId)
        assertNull(attribution.campaignMedium)
    }

    @Test
    fun mapToDomain_withPaidCampaignMediumWithoutClickId_isAttributed() {
        val attribution = installReferrerAttributionMapper.mapToDomain(
            installReferrer = "utm_source=instagram&utm_medium=social&utm_campaign=ukus3",
        )

        assertTrue(attribution.isAttributed)
        assertNull(attribution.googleClickId)
        assertEquals("ukus3", attribution.campaignName)
    }

    @Test
    fun mapToDomain_withOrganicPlayStoreReferrer_isNotAttributed() {
        val attribution = installReferrerAttributionMapper.mapToDomain(
            installReferrer = "utm_source=google-play&utm_medium=organic",
        )

        assertFalse(attribution.isAttributed)
        assertEquals("google-play", attribution.campaignSource)
    }

    @Test
    fun mapToDomain_withNotSetReferrer_isNotAttributed() {
        val attribution = installReferrerAttributionMapper.mapToDomain(
            installReferrer = "utm_source=(not%20set)&utm_medium=(not%20set)",
        )

        assertFalse(attribution.isAttributed)
    }

    @Test
    fun mapToDomain_withEmptyReferrer_isNotAttributed() {
        val attribution = installReferrerAttributionMapper.mapToDomain(installReferrer = "")

        assertFalse(attribution.isAttributed)
        assertNull(attribution.googleClickId)
        assertNull(attribution.campaignSource)
    }

    @Test
    fun mapToDomain_withBlankClickIdAndMedium_isNotAttributed() {
        val attribution = installReferrerAttributionMapper.mapToDomain(installReferrer = "gclid=&utm_medium=")

        assertFalse(attribution.isAttributed)
        assertNull(attribution.googleClickId)
        assertNull(attribution.campaignMedium)
    }

    @Test
    fun mapToDomain_withInviteCodeOnly_readsInviteCodeWithoutAttribution() {
        val attribution = installReferrerAttributionMapper.mapToDomain(installReferrer = "invite_code=$INVITE_CODE")

        assertFalse(attribution.isAttributed)
        assertEquals(INVITE_CODE, attribution.inviteCode)
        assertNull(attribution.googleClickId)
    }

    @Test
    fun mapToDomain_withInviteCodeAndGoogleAdsReferrer_keepsCampaignAttribution() {
        val attribution = installReferrerAttributionMapper.mapToDomain(
            installReferrer = "$GOOGLE_ADS_REFERRER&invite_code=$INVITE_CODE",
        )

        assertTrue(attribution.isAttributed)
        assertEquals(INVITE_CODE, attribution.inviteCode)
        assertEquals(GOOGLE_CLICK_ID, attribution.googleClickId)
        assertEquals("korea ad first", attribution.campaignName)
    }

    @Test
    fun mapToDomain_withoutInviteCode_hasNoInviteCode() {
        val attribution = installReferrerAttributionMapper.mapToDomain(installReferrer = GOOGLE_ADS_REFERRER)

        assertNull(attribution.inviteCode)
    }

    @Test
    fun mapToDomain_withBlankInviteCode_hasNoInviteCode() {
        val attribution = installReferrerAttributionMapper.mapToDomain(installReferrer = "invite_code=&utm_medium=organic")

        assertNull(attribution.inviteCode)
    }

    private companion object {
        const val INVITE_CODE = "ABC123"
        const val GOOGLE_CLICK_ID = "Cj0KCQjwtest"
        const val GOOGLE_ADS_REFERRER = "gclid=$GOOGLE_CLICK_ID&utm_source=google&utm_medium=cpc" +
            "&utm_campaign=korea%20ad%20first&utm_term=couple+app&utm_content=banner_1"
    }
}
