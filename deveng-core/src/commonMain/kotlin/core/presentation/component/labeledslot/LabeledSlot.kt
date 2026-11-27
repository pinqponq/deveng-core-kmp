package core.presentation.component.labeledslot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.RoundedSurface
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A container component that displays content with optional overlay labels positioned at various alignments.
 *
 * @param modifier Modifier to be applied to the container.
 * @param labels List of Label objects to display as overlays.
 * @param containerWidth Width of the container. If null, uses theme default.
 * @param containerHeight Height of the container. If null, uses theme default.
 * @param containerShape Shape of the container. If null, uses theme default.
 * @param labelPadding Padding for the text inside the label badges. Default is 5.dp.
 * @param labelTextStyle Text style for the labels. If null, uses theme default (Bold + theme size).
 * @param contentSlot Composable content displayed in the container (typically an image or view).
 */
@Composable
fun LabeledSlot(
    modifier: Modifier = Modifier,
    labels: List<Label> = emptyList(),
    containerWidth: Dp? = null,
    containerHeight: Dp? = null,
    containerShape: CornerBasedShape? = null,
    labelFontSize: TextUnit? = null,
    labelPadding: PaddingValues? = null,
    labelTextStyle: TextStyle? = null,
    contentSlot: @Composable () -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val labeledSlotTheme = componentTheme.labeledSlot
    val finalContainerWidth = containerWidth ?: labeledSlotTheme.containerWidth
    val finalContainerHeight = containerHeight ?: labeledSlotTheme.containerHeight
    val finalContainerShape = containerShape ?: labeledSlotTheme.containerShape
    val finalLabelPadding = labelPadding ?: labeledSlotTheme.labelPadding
    val textStyle = labelTextStyle ?: labeledSlotTheme.labelTextStyle
    val finalTextStyle = if (labelFontSize != null) {
        textStyle.copy(fontSize = labelFontSize)
    } else {
        textStyle
    }

    RoundedSurface(
        modifier = modifier
            .height(finalContainerHeight)
            .width(finalContainerWidth),
        shape = finalContainerShape
    ) {
        Box {
            contentSlot()

            labels.forEach { label ->
                val finalStyle = finalTextStyle.copy(
                    color = label.textColor
                )
                Box(
                    modifier = Modifier
                        .align(label.alignment)
                        .background(
                            color = label.backgroundColor,
                            shape = label.shape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier
                            .padding(finalLabelPadding),
                        text = label.text,
                        style = finalStyle
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun LabeledSlotPreview() {
    AppTheme {
        LabeledSlot(
            labels = listOf(
                Label(
                    text = "DEFAULT 5.dp",
                    alignment = Alignment.TopEnd,
                    backgroundColor = Color.Red,
                    textColor = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
            ),
            contentSlot = {
                Box(modifier = Modifier.background(Color.Gray))
            }
        )

        LabeledSlot(
            modifier = Modifier.padding(top = 120.dp),
            labels = listOf(
                Label(
                    text = "CUSTOM PADDING",
                    alignment = Alignment.BottomStart,
                    backgroundColor = Color.Blue,
                    textColor = Color.Yellow,
                    shape = RoundedCornerShape(4.dp)
                )
            ),
            labelPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
            labelTextStyle = TextStyle(fontSize = 14.sp),
            contentSlot = {
                Box(modifier = Modifier.background(Color.LightGray))
            }
        )
    }
}