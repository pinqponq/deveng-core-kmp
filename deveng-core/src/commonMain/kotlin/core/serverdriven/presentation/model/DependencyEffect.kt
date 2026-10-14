package core.serverdriven.presentation.model

enum class DependencyEffect {
    EDITABLE,
    VISIBILITY;

    companion object {
        fun fromValue(value: String): DependencyEffect? {
            return entries.firstOrNull { effect -> effect.name == value.uppercase() }
        }
    }
}
