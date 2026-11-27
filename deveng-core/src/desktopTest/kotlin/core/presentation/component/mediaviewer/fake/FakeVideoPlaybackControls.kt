package core.presentation.component.mediaviewer.fake

import chaintech.videoplayer.host.MediaPlayerEvent
import core.presentation.component.mediaviewer.VideoPlaybackControls

internal class FakeVideoPlaybackControls : VideoPlaybackControls {
    val calls = mutableListOf<String>()
    val seekedSeconds = mutableListOf<Float>()
    var eventListener: ((MediaPlayerEvent) -> Unit)? = null

    override fun play() {
        calls += PLAY
    }

    override fun pause() {
        calls += PAUSE
    }

    override fun seekTo(seconds: Float) {
        seekedSeconds += seconds
    }

    override fun setOnEvent(onEvent: ((MediaPlayerEvent) -> Unit)?) {
        eventListener = onEvent
    }

    fun send(event: MediaPlayerEvent) {
        eventListener?.invoke(event)
    }

    companion object {
        const val PLAY = "play"
        const val PAUSE = "pause"
    }
}
