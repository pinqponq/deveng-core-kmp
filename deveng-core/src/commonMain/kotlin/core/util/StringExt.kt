package core.util

val String.Companion.EMPTY: String get() = ""

fun String.clearWhiteSpace(): String {
    return if (this.isNotEmpty()) {
        this.replace("\\s".toRegex(), "")
    } else {
        this
    }
}

fun String.clearNonNumeric(allowedChars: List<Char> = emptyList()): String {
    return this.filter { it.isDigit() || allowedChars.contains(it) }
}

fun String.isValidEmail(): Boolean {
    val allowedLocalPartChars = "A-Za-z0-9+_.-"
    val allowedDomainChars = "A-Za-z0-9.-"
    val topLevelDomainPattern = "[A-Za-z]{2,}"

    val emailRegex =
        "^[${allowedLocalPartChars}]+@[${allowedDomainChars}]+\\.${topLevelDomainPattern}$".toRegex()
    return this.matches(emailRegex)
}
