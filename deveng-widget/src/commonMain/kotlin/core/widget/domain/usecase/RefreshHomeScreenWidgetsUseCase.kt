package core.widget.domain.usecase

import core.widget.domain.HomeScreenWidgetReloader

class RefreshHomeScreenWidgetsUseCase(
    private val homeScreenWidgetReloader: HomeScreenWidgetReloader,
) {
    operator fun invoke() {
        homeScreenWidgetReloader.reloadHomeScreenWidgets()
    }
}
