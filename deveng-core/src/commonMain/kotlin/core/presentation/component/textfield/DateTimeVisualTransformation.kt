package core.presentation.component.textfield

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class DateTimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 12) text.text.substring(0..11) else text.text
        val sb = StringBuilder()

        for (i in trimmed.indices) {
            sb.append(trimmed[i])
            when (i) {
                1, 3 -> sb.append('-')
                7 -> sb.append(' ')
                9 -> sb.append(':')
            }
        }

        val out = sb.toString()
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 7) return offset + 2
                if (offset <= 9) return offset + 3
                if (offset <= 11) return offset + 4
                return out.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                if (offset <= 12) return offset - 3
                if (offset <= 16) return offset - 4
                return 12
            }
        }

        return TransformedText(androidx.compose.ui.text.AnnotatedString(out), offsetMapping)
    }
}