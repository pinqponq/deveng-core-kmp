package core

import kotlinx.serialization.json.Json

inline fun <reified T> toJson(
    data: T,
    isUnknownKeysIgnored: Boolean = false
): String {
    val json = Json { ignoreUnknownKeys = isUnknownKeysIgnored }
    return json.encodeToString(data)
}

inline fun <reified T> fromJson(
    jsonString: String,
    isUnknownKeysIgnored: Boolean = false
): T {
    val json = Json { ignoreUnknownKeys = isUnknownKeysIgnored }
    return json.decodeFromString(jsonString)
}