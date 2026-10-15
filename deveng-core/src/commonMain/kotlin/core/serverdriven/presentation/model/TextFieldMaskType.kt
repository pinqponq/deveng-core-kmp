package core.serverdriven.presentation.model

enum class TextFieldMaskType {
    PHONE_NUMBER,
    TIME,
    DATE,
    DATE_AND_TIME;

    companion object {
        internal fun fromValue(value: String): TextFieldMaskType? {
            return entries.firstOrNull { maskType -> maskType.name.equals(value, ignoreCase = true) }
        }
    }
}
