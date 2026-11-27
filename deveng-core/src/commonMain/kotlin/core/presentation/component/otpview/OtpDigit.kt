package core.presentation.component.otpview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun OtpDigit(
    modifier: Modifier,
    value: String,
    isError: Boolean,
    isFocused: Boolean,
    onDigitClick: () -> Unit = {}
) {
    val componentTheme = LocalComponentTheme.current
    val otpViewTheme = componentTheme.otpView

    val shakeOffset = rememberShakeOffset(trigger = isError)

    val borderColor = when {
        isError -> otpViewTheme.errorBorderColor
        isFocused -> otpViewTheme.focusedBorderColor
        else -> otpViewTheme.unfocusedBorderColor
    }

    val textStyle = otpViewTheme.textStyle.copy(
        textAlign = TextAlign.Center,
        color = if (isError) otpViewTheme.errorTextColor else otpViewTheme.textColor,
        fontSize = 25.sp,
        fontWeight = FontWeight.Bold
    )

    Box(
        modifier = modifier
            .offset(x = shakeOffset.dp)
            .alpha(1f)
            .width(otpViewTheme.digitWidth)
            .height(otpViewTheme.digitHeight)
            .background(Color.Transparent, otpViewTheme.shape)
            .border(otpViewTheme.borderWidth, borderColor, otpViewTheme.shape)
            .clickable { onDigitClick() },
        contentAlignment = Alignment.Center
    ) {
        if (value.isNotEmpty()) {
            Text(text = value, style = textStyle)
        } else if (!isFocused) {
            DotShape(
                color = otpViewTheme.dotColor,
                size = otpViewTheme.dotSize
            )
        }
    }
}

@Composable
fun DotShape(
    color: Color = Color.Gray,
    size: Dp = 8.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color, RoundedCornerShape(50))
    )
}

@Preview
@Composable
fun OtpDigitPreview() {
    AppTheme {
        Column(
            Modifier.background(
                Color.Black
            )
        ) {
            OtpDigit(
                modifier = Modifier,
                value = "1",
                isError = false,
                isFocused = true
            )

            OtpDigit(
                modifier = Modifier,
                value = "",
                isError = false,
                isFocused = true
            )

            OtpDigit(
                modifier = Modifier,
                value = "",
                isError = true,
                isFocused = true
            )
        }
    }
}
