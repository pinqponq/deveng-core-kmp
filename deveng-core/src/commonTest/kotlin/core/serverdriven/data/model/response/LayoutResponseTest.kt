package core.serverdriven.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutResponseTest {

    private val layoutJson = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_withoutVersion_usesInitialVersion() {
        val layout = layoutJson.decodeFromString<LayoutResponse>("""{"components":[],"dependencies":[]}""")

        assertEquals(LayoutResponse.INITIAL_VERSION, layout.version)
    }

    @Test
    fun decode_withVersion_keepsIt() {
        val layout = layoutJson.decodeFromString<LayoutResponse>("""{"version":$NEWER_VERSION,"components":[]}""")

        assertEquals(NEWER_VERSION, layout.version)
    }

    private companion object {
        const val NEWER_VERSION = 2
    }
}
