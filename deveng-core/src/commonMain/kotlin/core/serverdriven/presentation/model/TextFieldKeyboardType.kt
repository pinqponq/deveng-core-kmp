package core.serverdriven.presentation.model

enum class TextFieldKeyboardType {
    TEXT,
    NUMERIC,
    PASSWORD,
    EMAIL;

    companion object {
        internal fun fromValue(value: String): TextFieldKeyboardType? {
            return entries.firstOrNull { keyboardType -> keyboardType.name.equals(value, ignoreCase = true) }
        }
    }
}
