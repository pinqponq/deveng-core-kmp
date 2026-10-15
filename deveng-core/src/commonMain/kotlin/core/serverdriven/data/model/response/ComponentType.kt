package core.serverdriven.data.model.response

internal enum class ComponentType(val serialName: String) {
    SHUTTER("shutter"),
    TABS("tabs"),
    DEPRECATED_TABS("tab"),
    SLIDER("slider"),
    SWITCH("switch"),
    TEXT_FIELD("textfield"),
    DROPDOWN("dropdown"),
    DIVIDER("divider"),
    ROW("row"),
    LABEL("label"),
    SEGMENTED("segmented"),
    RADIO("radio"),
    RATING("rating"),
    SECTION("section"),
    DATE("date");

    companion object {
        fun fromSerialName(serialName: String): ComponentType? {
            return entries.firstOrNull { componentType -> componentType.serialName == serialName }
        }
    }
}
