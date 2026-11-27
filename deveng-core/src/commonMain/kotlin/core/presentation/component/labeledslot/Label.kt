package core.presentation.component.labeledslot

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

data class Label(
    val text: String,
    val textColor: Color,
    val backgroundColor: Color,
    val alignment: Alignment,
    val shape: RoundedCornerShape
)
