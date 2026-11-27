package core.session.domain

import core.session.domain.model.AuthTokens
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration

class AuthSessionOptionsTest {

    @Test
    fun create_withZeroRevokeTimeout_throws() {
        assertFailsWith<IllegalArgumentException> {
            AuthSessionOptions(
                renewTokens = { AuthTokens(accessToken = ACCESS_TOKEN) },
                onAccessTokenChanged = {},
                revokeTimeout = Duration.ZERO,
            )
        }
    }

    private companion object {
        const val ACCESS_TOKEN = "access-token"
    }
}
