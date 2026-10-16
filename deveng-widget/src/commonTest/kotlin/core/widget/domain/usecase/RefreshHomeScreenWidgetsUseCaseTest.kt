package core.widget.domain.usecase

import core.widget.fake.FakeHomeScreenWidgetReloader
import kotlin.test.Test
import kotlin.test.assertEquals

class RefreshHomeScreenWidgetsUseCaseTest {

    @Test
    fun invoke_reloadsTheWidgetsOnce() {
        val widgetReloader = FakeHomeScreenWidgetReloader()

        RefreshHomeScreenWidgetsUseCase(homeScreenWidgetReloader = widgetReloader)()

        assertEquals(1, widgetReloader.reloadCount)
    }
}
