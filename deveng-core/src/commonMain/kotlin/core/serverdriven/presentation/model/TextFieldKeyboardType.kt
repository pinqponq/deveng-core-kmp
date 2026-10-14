package core.serverdriven.presentation.model

enum class TextFieldKeyboardType {
    TEXT,
    NUMERIC,
    PASSWORD,
    EMAIL;

    companion object {
        fun fromValue(value: String): TextFieldKeyboardType? {
            return entries.firstOrNull { keyboardType -> keyboardType.name == value.uppercase() }
        }
    }
}
