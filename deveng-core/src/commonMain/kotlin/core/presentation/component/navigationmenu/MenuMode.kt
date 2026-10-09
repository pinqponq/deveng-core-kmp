package core.presentation.component.navigationmenu

/**
 * Display mode options for the menu component.
 */
enum class MenuMode {
    /**
     * Vertical side menu mode with expand/collapse functionality.
     */
    Vertical,

    /**
     * Horizontal header mode, always expanded and fixed.
     */
    Horizontal,

    /**
     * Compact bar at the bottom of the window with the items in a row.
     */
    BottomBar,

    /**
     * Compact bar as a narrow column at the start or end of the window, positioned by [MenuAlignment].
     */
    SideBar
}
