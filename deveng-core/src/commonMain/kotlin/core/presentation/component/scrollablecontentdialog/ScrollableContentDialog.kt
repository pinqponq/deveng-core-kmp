package core.presentation.component.scrollablecontentdialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import core.presentation.component.CustomIconButton
import core.presentation.component.alertdialog.CustomDialog
import core.presentation.theme.LocalComponentTheme
import core.util.markdown.MarkdownContentParser
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_cont_desc_icon_check_box
import global.deveng.deveng_core.generated.resources.shared_content_desc_icon_left
import global.deveng.deveng_core.generated.resources.shared_ic_arrow_left
import global.deveng.deveng_core.generated.resources.shared_ic_checked_circle
import global.deveng.deveng_core.generated.resources.shared_ic_unchecked_circle
import global.deveng.deveng_core.generated.resources.shared_read_and_approve_text
import org.jetbrains.compose.resources.stringResource

private val EdgeFadeAlphaStops = listOf(0f, 0.3f, 0.7f, 1f)
private val HeaderTopPadding = 25.dp
private val HeaderBottomPadding = 15.dp
private val BackIconHeight = 14.dp
private val BackIconWidth = 18.dp
private val TitleHorizontalPadding = 45.dp
private val ContentHorizontalPadding = 20.dp
private val ContentBottomPadding = 13.dp
private val ApprovalRowTopPadding = 15.dp
private val ApprovalRowBottomPadding = 41.dp
private const val HEADER_Z_INDEX = 2f

/**
 * Full-height dialog that shows markdown content under a fixed header, fading the content out at the
 * scroll edges, with an optional "read and approve" checkbox row at the end.
 *
 * @param title Title shown in the header and at the start of the approval text.
 * @param content Markdown content rendered with [MarkdownContentParser].
 * @param isChecked Whether the approval checkbox is checked.
 * @param isDialogVisible Whether the dialog is shown.
 * @param isCheckboxVisible Whether the approval checkbox row is shown after the content.
 * @param backIconDescription Content description of the back icon.
 * @param checkboxIconDescription Content description of the checkbox icon.
 * @param approvalText Text appended after [title] in the approval row.
 * @param onBackIconClick Callback invoked when the back icon is clicked.
 * @param onDismissRequest Callback invoked when the dialog should be dismissed.
 * @param onCheckIconClick Callback invoked when the checkbox icon is clicked.
 */
@Composable
fun ScrollableContentDialog(
    title: String,
    content: String,
    isChecked: Boolean = false,
    isDialogVisible: Boolean,
    isCheckboxVisible: Boolean = false,
    backIconDescription: String = stringResource(Res.string.shared_content_desc_icon_left),
    checkboxIconDescription: String = stringResource(Res.string.shared_cont_desc_icon_check_box),
    approvalText: String = stringResource(Res.string.shared_read_and_approve_text),
    onBackIconClick: () -> Unit,
    onDismissRequest: () -> Unit,
    onCheckIconClick: () -> Unit = {}
) {
    val dialogTheme = LocalComponentTheme.current.scrollableContentDialog
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    var headerHeightDp by remember { mutableStateOf(0.dp) }

    val isScrollAtTop by remember { derivedStateOf { scrollState.value == 0 } }
    val isScrollAtBottom by remember { derivedStateOf { scrollState.value >= scrollState.maxValue } }

    if (isDialogVisible) {
        CustomDialog(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = dialogTheme.verticalPadding)
                .shadow(
                    elevation = dialogTheme.shadowElevation,
                    shape = RoundedCornerShape(dialogTheme.cornerRadius),
                    clip = false
                ),
            onDismissRequest = onDismissRequest
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(dialogTheme.backgroundColor)
                        .align(Alignment.TopCenter)
                        .zIndex(HEADER_Z_INDEX)
                        .onSizeChanged { size ->
                            headerHeightDp = with(density) { size.height.toDp() }
                        }
                        .padding(top = HeaderTopPadding, bottom = HeaderBottomPadding)
                ) {
                    CustomIconButton(
                        modifier = Modifier.align(Alignment.CenterStart),
                        iconModifier = Modifier.height(BackIconHeight).width(BackIconWidth),
                        backgroundColor = Color.Transparent,
                        icon = Res.drawable.shared_ic_arrow_left,
                        iconDescription = backIconDescription,
                        iconTint = dialogTheme.backIconTint,
                        onClick = onBackIconClick
                    )

                    Text(
                        text = title,
                        style = dialogTheme.titleTextStyle,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = TitleHorizontalPadding)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(dialogTheme.backgroundColor)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()

                            val currentHeaderHeightPx = with(density) { headerHeightDp.toPx() }
                            val fadeHeightPx = dialogTheme.edgeFadeHeight.toPx()
                            val contentHeight = size.height

                            if (!isScrollAtTop) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = EdgeFadeAlphaStops.map { stopAlpha -> Color.Black.copy(alpha = stopAlpha) },
                                        startY = currentHeaderHeightPx,
                                        endY = currentHeaderHeightPx + fadeHeightPx
                                    ),
                                    topLeft = Offset(0f, currentHeaderHeightPx),
                                    size = Size(size.width, fadeHeightPx),
                                    blendMode = BlendMode.DstIn
                                )
                            }

                            if (!isScrollAtBottom) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = contentHeight - fadeHeightPx,
                                        endY = contentHeight
                                    ),
                                    topLeft = Offset(0f, contentHeight - fadeHeightPx),
                                    size = Size(size.width, fadeHeightPx),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                        .verticalScroll(scrollState)
                        .padding(
                            top = headerHeightDp,
                            start = ContentHorizontalPadding,
                            end = ContentHorizontalPadding,
                            bottom = ContentBottomPadding
                        )
                ) {
                    MarkdownContentParser(
                        markdownContent = content,
                        textColor = dialogTheme.contentTextColor
                    )

                    if (isCheckboxVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = ApprovalRowTopPadding, bottom = ApprovalRowBottomPadding),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomIconButton(
                                icon = if (isChecked) Res.drawable.shared_ic_checked_circle else Res.drawable.shared_ic_unchecked_circle,
                                iconModifier = Modifier,
                                backgroundColor = dialogTheme.checkboxBackgroundColor,
                                iconDescription = checkboxIconDescription,
                                iconTint = dialogTheme.checkboxIconTint,
                                onClick = { onCheckIconClick() }
                            )
                            Text(
                                modifier = Modifier.weight(1f),
                                text = buildAnnotatedString {
                                    withStyle(dialogTheme.approvalTitleTextStyle.toSpanStyle()) {
                                        append(title)
                                    }
                                    append(" ")
                                    withStyle(dialogTheme.approvalTextStyle.toSpanStyle()) {
                                        append(approvalText)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
