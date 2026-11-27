package core.presentation.component.textfield

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class InlineSuffixTransformation(
    private val suffix: String,
    private val suffixTextStyle: TextStyle? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (suffix.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val spanStyle = suffixTextStyle?.let(::textStyleToSpanStyle)

        val transformed = buildAnnotatedString {
            append(text)
            if (spanStyle != null) {
                pushStyle(spanStyle)
                append(suffix)
                pop()
            } else {
                append(suffix)
            }
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int =
                offset.coerceAtMost(text.length)
        }

        return TransformedText(transformed, mapping)
    }
}

private fun textStyleToSpanStyle(textStyle: TextStyle): SpanStyle = SpanStyle(
    color = textStyle.color,
    fontSize = textStyle.fontSize,
    fontWeight = textStyle.fontWeight,
    fontStyle = textStyle.fontStyle,
    fontSynthesis = textStyle.fontSynthesis,
    fontFamily = textStyle.fontFamily,
    fontFeatureSettings = textStyle.fontFeatureSettings,
    letterSpacing = textStyle.letterSpacing,
    baselineShift = textStyle.baselineShift,
    textGeometricTransform = textStyle.textGeometricTransform,
    localeList = textStyle.localeList,
    background = textStyle.background,
    textDecoration = textStyle.textDecoration,
    shadow = textStyle.shadow
)

