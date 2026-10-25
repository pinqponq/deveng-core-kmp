package core.presentation.component.scrolltoedgebutton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import core.presentation.theme.CoreSemiBoldTextStyle
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_desc_unread_count
import org.jetbrains.compose.resources.stringResource

internal const val SCROLL_EDGE_BADGE_MAX_COUNT = 999

internal fun scrollToEdgeBadgeText(count: Int): String =
    if (count > SCROLL_EDGE_BADGE_MAX_COUNT) "$SCROLL_EDGE_BADGE_MAX_COUNT+" else count.toString()

@Composable
internal fun ScrollToEdgeBadge(count: Int, modifier: Modifier = Modifier) {
    val theme = LocalComponentTheme.current.scrollToEdgeButton
    val text = scrollToEdgeBadgeText(count = count)
    val description = stringResource(Res.string.shared_content_desc_unread_count, text)
    Box(
        modifier = modifier
            .offset(x = 4.dp, y = (-4).dp)
            .defaultMinSize(minWidth = theme.badgeMinSize, minHeight = theme.badgeMinSize)
            .background(color = theme.badgeBackgroundColor, shape = CircleShape)
            .padding(horizontal = 5.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = theme.badgeTextColor,
            style = CoreSemiBoldTextStyle().copy(fontSize = theme.badgeTextSize),
        )
    }
}
