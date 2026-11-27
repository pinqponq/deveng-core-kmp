package core.analytics.data.client.googletag

import core.analytics.domain.model.CommonAnalyticsEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GoogleTagOptionsTest {

    @Test
    fun construct_withBlankMeasurementId_fails() {
        assertFailsWith<IllegalArgumentException> { GoogleTagOptions(measurementId = " ") }
        assertFailsWith<IllegalArgumentException> { GoogleTagOptions(measurementId = "") }
    }

    @Test
    fun construct_withBlankHostname_fails() {
        assertFailsWith<IllegalArgumentException> {
            GoogleTagOptions(measurementId = MEASUREMENT_ID, enabledHostnames = setOf("app.example.com", " "))
        }
    }

    @Test
    fun construct_withInvalidGlobalObjectName_fails() {
        listOf("", "1tag", "my tag", "tag.analytics", "tag-1").forEach { name ->
            assertFailsWith<IllegalArgumentException>("'$name' must be rejected") {
                GoogleTagOptions(measurementId = MEASUREMENT_ID, globalObjectName = name)
            }
        }
    }

    @Test
    fun construct_withOnlyMeasurementId_usesNeutralDefaults() {
        val options = GoogleTagOptions(measurementId = MEASUREMENT_ID)

        assertTrue(options.enabledHostnames.isEmpty())
        assertEquals(null, options.consent)
        assertEquals(GoogleTagOptions.DEFAULT_GLOBAL_OBJECT_NAME, options.globalObjectName)
        assertEquals(CommonAnalyticsEvent.LOGIN_COMPLETED.firebaseName, options.eventName(CommonAnalyticsEvent.LOGIN_COMPLETED))
    }

    @Test
    fun isEnabledOn_withListedHostname_returnsTrue() {
        val options = GoogleTagOptions(measurementId = MEASUREMENT_ID, enabledHostnames = setOf("app.example.com"))

        assertTrue(options.isEnabledOn("app.example.com"))
    }

    @Test
    fun isEnabledOn_withOtherHostnameOrNoHostnames_returnsFalse() {
        val options = GoogleTagOptions(measurementId = MEASUREMENT_ID, enabledHostnames = setOf("app.example.com"))

        assertFalse(options.isEnabledOn("localhost"))
        assertFalse(options.isEnabledOn("evil-app.example.com"))
        assertFalse(GoogleTagOptions(measurementId = MEASUREMENT_ID).isEnabledOn("app.example.com"))
    }

    @Test
    fun constructConsent_withBlankStorageKeyOrCssClassPrefix_fails() {
        assertFailsWith<IllegalArgumentException> { ConsentOptions(storageKey = " ") }
        assertFailsWith<IllegalArgumentException> { ConsentOptions(storageKey = "consent", cssClassPrefix = "") }
    }

    @Test
    fun construct_withAllSettings_exposesThem() {
        val consent = ConsentOptions(storageKey = "consent")
        val screenView = ScreenViewOptions(pathPrefix = "/app/", titlePrefix = "App · ")
        val options = GoogleTagOptions(
            measurementId = MEASUREMENT_ID,
            enabledHostnames = setOf("app.example.com"),
            consent = consent,
            screenView = screenView,
            eventName = { event -> event.pinqloqName.lowercase() },
            globalObjectName = "customTag",
            numericParameterNames = setOf("price")
        )

        assertEquals(MEASUREMENT_ID, options.measurementId)
        assertEquals(setOf("app.example.com"), options.enabledHostnames)
        assertSame(consent, options.consent)
        assertSame(screenView, options.screenView)
        assertEquals("customTag", options.globalObjectName)
        assertEquals(setOf("price"), options.numericParameterNames)
        assertEquals("login_completed", options.eventName(CommonAnalyticsEvent.LOGIN_COMPLETED))
    }

    @Test
    fun construct_withoutNumericParameterNames_usesValueAndTax() {
        assertEquals(setOf("value", "tax"), GoogleTagOptions(measurementId = MEASUREMENT_ID).numericParameterNames)
        assertEquals(setOf("value", "tax"), GoogleTagOptions.DEFAULT_NUMERIC_PARAMETER_NAMES)
    }

    private companion object {
        const val MEASUREMENT_ID = "G-TEST000000"
    }
}
