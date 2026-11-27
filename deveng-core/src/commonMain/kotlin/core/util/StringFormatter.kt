package core.util

class StringFormatter {
    
    /**
     * @param input The string to format
     * @param clearNonAlphabetic Remove non-alphabetic characters (default: false)
     * @param clearNonNumeric Remove non-numeric characters (default: false)
     * @param clearWhiteSpace Remove all whitespace characters (default: false)
     * @param filterConsecutiveSpaces Replace multiple consecutive spaces with single space (default: false)
     * @param clearSpecialChars Remove special characters except allowed ones (default: false)
     * @param allowedSpecialChars List of characters to keep when clearing special chars (default: empty)
     * @param isWhitespaceAllowed Whether to allow whitespace when clearing non-alphabetic (default: true)
     * @return Formatted string
     */
    fun formatInput(
        input: String,
        clearNonAlphabetic: Boolean = false,
        clearNonNumeric: Boolean = false,
        clearWhiteSpace: Boolean = false,
        filterConsecutiveSpaces: Boolean = false,
        isWhitespaceAllowed: Boolean = true,
        clearSpecialChars: Boolean = false,
        allowedSpecialChars: List<Char> = emptyList()
    ): String {
        var formattedInput = input

        if (clearNonAlphabetic) {
            formattedInput = formattedInput.clearNonAlphabetic(allowedSpecialChars, isWhitespaceAllowed)
        }

        if (clearNonNumeric) {
            formattedInput = formattedInput.clearNonNumeric(allowedSpecialChars)
        }

        if (clearWhiteSpace) {
            formattedInput = formattedInput.clearWhiteSpace()
        }

        if (clearSpecialChars) {
            formattedInput = formattedInput.clearSpecialChars(allowedSpecialChars)
        }

        if (filterConsecutiveSpaces) {
            formattedInput = formattedInput.filterConsecutiveSpaces()
        }
        
        return formattedInput
    }

    private fun String.clearWhiteSpace(): String {
        return if (this.isNotEmpty()) {
            this.replace("\\s".toRegex(), "")
        } else {
            this
        }
    }
    
    private fun String.clearSpecialChars(exceptionChars: List<Char>? = null): String {
        if (this.isBlank() || this.isEmpty() || exceptionChars?.contains(this.last()) == true) {
            return this
        }
        
        return this.filter { it.isLetterOrDigit() || it.isWhitespace() || exceptionChars?.contains(it) == true }
    }
    
    private fun String.filterConsecutiveSpaces(): String {
        return this.replace(Regex(" +"), " ")
    }
    
    private fun String.clearNonNumeric(allowedChars: List<Char> = emptyList()): String {
        return this.filter { it.isDigit() || allowedChars.contains(it) }
    }
    
    private fun String.clearNonAlphabetic(
        allowedChars: List<Char> = emptyList(),
        isWhitespaceAllowed: Boolean = true
    ): String {
        return this.filter {
            it.isLetter() ||
                    (isWhitespaceAllowed && it.isWhitespace()) ||
                    allowedChars.contains(it)
        }
    }
}
