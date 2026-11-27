package core.linking

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkBuilderTest {
    @Test
    fun build_withPathSegments_joinsThemUnderSchemeAndHost() {
        val link = LinkBuilder(host = "example.app").build("branch", 7)

        assertEquals("https://example.app/branch/7", link)
    }

    @Test
    fun build_withCustomScheme_usesIt() {
        val link = LinkBuilder(host = "profile", scheme = "app").build()

        assertEquals("app://profile", link)
    }
}
