package core.serverdriven.presentation.model

internal enum class DependencyOperator {
    EQUALS,
    LESS_THAN,
    GREATER_THAN;

    companion object {
        fun fromValue(value: String): DependencyOperator? {
            return entries.firstOrNull { operator -> operator.name.equals(value, ignoreCase = true) }
        }
    }
}
