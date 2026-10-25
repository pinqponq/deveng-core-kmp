package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomIconButton
import core.presentation.motion.rememberSwipeToReplyState
import core.presentation.motion.swipeToReply
import core.presentation.theme.CoreMediumTextStyle
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import deveng_core_kmp.sample.composeapp.generated.resources.ic_reply
import org.jetbrains.compose.resources.painterResource

private const val SWIPE_TO_REPLY_DEMO_MESSAGE_COUNT = 60
private const val SWIPE_TO_REPLY_DEMO_ICON_EXTRA_SCALE = 0.2f
private val SWIPE_TO_REPLY_DEMO_SENT_COLOR = Color(0xFFD9F2E6)
private val SWIPE_TO_REPLY_DEMO_RECEIVED_COLOR = Color(0xFFEDEDED)

/**
 * Playground for `Modifier.swipeToReply`: a long chat-like list where every message can be swiped
 * towards the end edge to reply while the list still scrolls up and down. The reply icon next to a
 * message grows with the swipe, and the banner at the top shows the last message replied to.
 */
@Composable
internal fun SwipeToReplyDemoScreen(onBack: () -> Unit) {
    var repliedMessageNumber by remember { mutableStateOf<Int?>(null) }
    var replyCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            CustomIconButton(
                icon = Res.drawable.ic_arrow_left,
                iconDescription = "Back",
                onClick = onBack,
            )

            Column {
                Text(
                    text = repliedMessageNumber?.let { "Replying to message #$it" } ?: "Swipe a message to the right to reply",
                    style = CoreMediumTextStyle().copy(fontSize = 16.sp),
                )

                Text(
                    text = "Replies: $replyCount",
                    style = CoreMediumTextStyle().copy(fontSize = 13.sp, color = Color.Gray),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            items(count = SWIPE_TO_REPLY_DEMO_MESSAGE_COUNT) { index ->
                SwipeToReplyDemoMessage(
                    messageNumber = index,
                    isSentMessage = index % 2 == 0,
                    onReply = {
                        repliedMessageNumber = index
                        replyCount++
                    },
                )
            }
        }
    }
}

@Composable
private fun SwipeToReplyDemoMessage(
    messageNumber: Int,
    isSentMessage: Boolean,
    onReply: () -> Unit,
) {
    val swipeState = rememberSwipeToReplyState()
    Box(modifier = Modifier.fillMaxWidth()) {
        Icon(
            painter = painterResource(Res.drawable.ic_reply),
            contentDescription = null,
            tint = Color(0xFF00897B),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .graphicsLayer {
                    val iconScale = 1f + SWIPE_TO_REPLY_DEMO_ICON_EXTRA_SCALE * swipeState.triggerProgress
                    alpha = swipeState.triggerProgress
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )

        Text(
            text = "Message #$messageNumber",
            style = CoreMediumTextStyle().copy(fontSize = 16.sp),
            modifier = Modifier
                .align(if (isSentMessage) Alignment.CenterEnd else Alignment.CenterStart)
                .swipeToReply(state = swipeState, onTrigger = onReply)
                .widthIn(min = 160.dp)
                .background(
                    color = if (isSentMessage) SWIPE_TO_REPLY_DEMO_SENT_COLOR else SWIPE_TO_REPLY_DEMO_RECEIVED_COLOR,
                    shape = RoundedCornerShape(size = 18.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
