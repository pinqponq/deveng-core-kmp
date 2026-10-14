package core.serverdriven.presentation.engine

import core.serverdriven.data.model.response.LayoutResponse

fun interface ShutterContentLoader {
    suspend fun loadShutterContent(shutterId: String): LayoutResponse
}
