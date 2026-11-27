package core.util

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.debouncedCombinedClickable(
    debounceMillis: Long = 600L,
    isDebounce: Boolean = true,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    shape: Shape? = null,
    onClick: () -> Unit
): Modifier = composed {
    val clickLatest by rememberUpdatedState(onClick)
    val longLatest by rememberUpdatedState(onLongClick)

    var canClick by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    val modifierWithClip = if (shape != null) {
        this.clip(shape)
    } else {
        this
    }

    modifierWithClip.combinedClickable(
        enabled = enabled,
        interactionSource = interactionSource,
        onClick = {
            if (!canClick) return@combinedClickable
            canClick = false
            clickLatest()
            scope.launch {
                if (isDebounce) {
                    delay(debounceMillis)
                }
                canClick = true
            }
        },
        onLongClick = { longLatest?.invoke() }
    )
}

fun Modifier.disableSplitMotionEvents() =
    pointerInput(Unit) {
        coroutineScope {
            var currentId: Long = -1L
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { pointerInfo ->
                        when {
                            pointerInfo.pressed && currentId == -1L -> currentId =
                                pointerInfo.id.value

                            pointerInfo.pressed.not() && currentId == pointerInfo.id.value -> currentId =
                                -1

                            pointerInfo.id.value != currentId && currentId != -1L -> pointerInfo.consume()
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

@Composable
fun Modifier.ifTrue(
    condition: Boolean,
    block: @Composable Modifier.() -> Modifier
): Modifier = if (condition) block() else this