package core.util

/**
 * RFC 3986 percent-encoder for a single URI component. Kotlin commonMain has no shared URLEncoder,
 * so UTF-8 bytes are encoded inline: unreserved characters (A-Z a-z 0-9 - _ . ~) stay as they are
 * and everything else becomes %XX — a space included, which is why this is not interchangeable with
 * the form-urlencoded variant that writes a space as '+'. Deep link parsers do not agree on what a
 * '+' means inside a query value, so the escape has to be unambiguous here.
 */
fun String.percentEncodeUriComponent(): String = buildString(capacity = length + 8) {
    for (character in this@percentEncodeUriComponent) {
        val isUnreserved = character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' ||
            character == '-' || character == '_' || character == '.' || character == '~'

        if (isUnreserved) {
            append(character)
            continue
        }

        for (byteValue in character.toString().encodeToByteArray()) {
            val unsignedByte = byteValue.toInt() and 0xFF
            append('%')
            append(nibbleToHexDigit(nibble = (unsignedByte shr 4) and 0xF))
            append(nibbleToHexDigit(nibble = unsignedByte and 0xF))
        }
    }
}

private fun nibbleToHexDigit(nibble: Int): Char =
    (if (nibble < 10) '0'.code + nibble else 'A'.code + nibble - 10).toChar()
