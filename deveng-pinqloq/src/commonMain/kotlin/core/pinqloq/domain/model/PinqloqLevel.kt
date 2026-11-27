package core.pinqloq.domain.model

enum class PinqloqLevel(val backendValue: String) {
    Debug("Debug"),
    Information("Information"),
    Warning("Warning"),
    Error("Error"),
    Fatal("Fatal")
}
