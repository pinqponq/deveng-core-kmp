package core.util

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class JwtClaimsTest {

    @Test
    fun decodeJwtPayload_withBase64UrlCharacters_returnsPayloadJson() {
        val token = createToken(URL_SAFE_PAYLOAD_JSON)

        val encodedPayload = token.split(".")[1]
        assertTrue(encodedPayload.contains('-') && encodedPayload.contains('_'))
        assertEquals(URL_SAFE_PAYLOAD_JSON, decodeJwtPayload(token))
    }

    @Test
    fun decodeJwtPayload_withoutPadding_returnsPayloadJson() {
        val token = createToken(SHORT_PAYLOAD_JSON)

        assertTrue(SHORT_PAYLOAD_JSON.length % BASE64_BLOCK_BYTE_COUNT != 0)
        assertEquals(SHORT_PAYLOAD_JSON, decodeJwtPayload(token))
    }

    @Test
    fun decodeJwtPayload_withPadding_returnsPayloadJson() {
        val paddedPayload = Base64.UrlSafe.encode(SHORT_PAYLOAD_JSON.encodeToByteArray())
        val token = "$HEADER_PART.$paddedPayload.$SIGNATURE_PART"

        assertTrue(paddedPayload.endsWith('='))
        assertEquals(SHORT_PAYLOAD_JSON, decodeJwtPayload(token))
    }

    @Test
    fun decodeJwtPayload_withTwoParts_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            decodeJwtPayload("$HEADER_PART.${encodePayload(SHORT_PAYLOAD_JSON)}")
        }
    }

    @Test
    fun decodeJwtPayload_withFourParts_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            decodeJwtPayload("${createToken(SHORT_PAYLOAD_JSON)}.$SIGNATURE_PART")
        }
    }

    @Test
    fun decodeJwtPayload_withInvalidBase64Payload_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            decodeJwtPayload("$HEADER_PART.$INVALID_BASE64_PART.$SIGNATURE_PART")
        }
    }

    @Test
    fun decodeJwtClaims_withUnknownClaims_ignoresThemAndReturnsKnownClaims() {
        val token = createToken("""{"sub":"$SUBJECT","role":"admin","iat":1700000000}""")

        assertEquals(SubjectClaims(subject = SUBJECT), decodeJwtClaims<SubjectClaims>(token))
    }

    @Test
    fun decodeJwtClaims_withStrictJsonAndUnknownClaims_throwsSerializationException() {
        val token = createToken("""{"sub":"$SUBJECT","role":"admin"}""")

        assertFailsWith<SerializationException> {
            decodeJwtClaims<SubjectClaims>(token, json = Json)
        }
    }

    @Test
    fun decodeJwtClaims_withInvalidJsonPayload_throwsSerializationException() {
        val token = createToken(INVALID_JSON_PAYLOAD)

        assertFailsWith<SerializationException> {
            decodeJwtClaims<SubjectClaims>(token)
        }
    }

    @Test
    fun jwtExpiresAt_withExpClaim_returnsExpiryInstant() {
        val token = createToken("""{"sub":"$SUBJECT","exp":$EXPIRES_AT_EPOCH_SECONDS}""")

        assertEquals(Instant.fromEpochSeconds(EXPIRES_AT_EPOCH_SECONDS), jwtExpiresAt(token))
    }

    @Test
    fun jwtExpiresAt_withFractionalExpClaim_keepsMilliseconds() {
        val token = createToken("""{"exp":$EXPIRES_AT_EPOCH_SECONDS.25}""")

        val expected = Instant.fromEpochMilliseconds(EXPIRES_AT_EPOCH_SECONDS * 1_000 + 250)
        assertEquals(expected, jwtExpiresAt(token))
    }

    @Test
    fun jwtExpiresAt_withoutExpClaim_returnsNull() {
        val token = createToken("""{"sub":"$SUBJECT"}""")

        assertNull(jwtExpiresAt(token))
    }

    @Test
    fun jwtExpiresAt_withNonNumericExpClaim_throwsSerializationException() {
        val token = createToken("""{"exp":"tomorrow"}""")

        assertFailsWith<SerializationException> {
            jwtExpiresAt(token)
        }
    }

    @Serializable
    private data class SubjectClaims(
        @SerialName("sub")
        val subject: String
    )

    private companion object {
        const val HEADER_PART = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        const val SIGNATURE_PART = "c2lnbmF0dXJl"
        const val INVALID_BASE64_PART = "not*base64"
        const val SUBJECT = "user-42"
        const val EXPIRES_AT_EPOCH_SECONDS = 1_800_000_000L
        const val BASE64_BLOCK_BYTE_COUNT = 3
        const val SHORT_PAYLOAD_JSON = """{"sub":"a"}"""
        const val URL_SAFE_PAYLOAD_JSON = """{"note":">>>???"}"""
        const val INVALID_JSON_PAYLOAD = """{"sub":"""

        val payloadEncoder = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

        fun encodePayload(payloadJson: String): String = payloadEncoder.encode(payloadJson.encodeToByteArray())

        fun createToken(payloadJson: String): String = "$HEADER_PART.${encodePayload(payloadJson)}.$SIGNATURE_PART"
    }
}
