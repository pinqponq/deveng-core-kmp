package core.linking

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalUriHandlerTest {
    @AfterTest
    fun tearDown() {
        ExternalUriHandler.listener = {}
        ExternalUriHandler.listener = null
    }

    @Test
    fun onNewUri_withListener_deliversUriImmediately() {
        val received = mutableListOf<String>()
        ExternalUriHandler.listener = { received += it }

        ExternalUriHandler.onNewUri("app://a")

        assertEquals(listOf("app://a"), received)
    }

    @Test
    fun listener_setAfterUriArrived_receivesCachedUriOnce() {
        ExternalUriHandler.onNewUri("app://cached")
        val received = mutableListOf<String>()

        ExternalUriHandler.listener = { received += it }
        ExternalUriHandler.listener = { received += it }

        assertEquals(listOf("app://cached"), received)
    }

    @Test
    fun onNewUri_withTwoUrisBeforeListener_keepsOnlyTheLatest() {
        ExternalUriHandler.onNewUri("app://first")
        ExternalUriHandler.onNewUri("app://second")
        val received = mutableListOf<String>()

        ExternalUriHandler.listener = { received += it }

        assertEquals(listOf("app://second"), received)
    }

    @Test
    fun onNewUri_afterDeliveredToListener_isNotReplayedToNextListener() {
        ExternalUriHandler.listener = {}
        ExternalUriHandler.onNewUri("app://seen")
        ExternalUriHandler.listener = null
        val received = mutableListOf<String>()

        ExternalUriHandler.listener = { received += it }

        assertEquals(emptyList(), received)
    }
}
