package core.util

import kotlin.io.encoding.Base64
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val JWT_PART_COUNT = 3
private const val JWT_PAYLOAD_PART_INDEX = 1
private const val MILLISECONDS_PER_SECOND = 1_000

private val jwtPayloadDecoder = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

@PublishedApi
internal val jwtClaimsJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class JwtExpirationClaim(
    @SerialName("exp")
    val expiresAtEpochSeconds: Double? = null
)

/**
 * Returns the JSON payload of [token] (the middle part, base64url with or without padding).
 *
 * The signature is not verified, so use the result only to read claims on the client, never to trust them.
 *
 * @throws IllegalArgumentException when [token] does not have three parts or its payload is not valid base64url.
 */
fun decodeJwtPayload(token: String): String {
    val tokenParts = token.split(".")
    require(tokenParts.size == JWT_PART_COUNT) {
        "Invalid JWT format: expected $JWT_PART_COUNT parts but found ${tokenParts.size}"
    }

    return jwtPayloadDecoder.decode(tokenParts[JWT_PAYLOAD_PART_INDEX]).decodeToString()
}

/**
 * Decodes the payload of [token] into [T]. Unknown claims are ignored by default.
 *
 * The signature is not verified, so use the result only to read claims on the client, never to trust them.
 *
 * @throws IllegalArgumentException when the token format is invalid or the payload does not match [T]
 * (a `SerializationException` is an `IllegalArgumentException`).
 */
inline fun <reified T> decodeJwtClaims(token: String, json: Json = jwtClaimsJson): T =
    json.decodeFromString(decodeJwtPayload(token))

/**
 * Returns the time in the standard `exp` claim of [token], or `null` when the token has no `exp` claim.
 *
 * The signature is not verified, so use the result only for client-side decisions such as refreshing early.
 *
 * @throws IllegalArgumentException when the token format is invalid or `exp` is not a number.
 */
fun jwtExpiresAt(token: String): Instant? {
    val expiresAtEpochSeconds = decodeJwtClaims<JwtExpirationClaim>(token).expiresAtEpochSeconds ?: return null
    val expiresAtEpochMilliseconds = (expiresAtEpochSeconds * MILLISECONDS_PER_SECOND).toLong()
    return Instant.fromEpochMilliseconds(expiresAtEpochMilliseconds)
}
