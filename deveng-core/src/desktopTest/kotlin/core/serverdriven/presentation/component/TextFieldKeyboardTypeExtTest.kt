package core.serverdriven.presentation.component

import androidx.compose.ui.text.input.KeyboardType
import core.serverdriven.presentation.model.TextFieldKeyboardType
import kotlin.test.Test
import kotlin.test.assertEquals

class TextFieldKeyboardTypeExtTest {

    @Test
    fun toComposeKeyboardType_forEveryType_mapsToMatchingComposeType() {
        val composeKeyboardTypes = TextFieldKeyboardType.entries.map { keyboardType -> keyboardType.toComposeKeyboardType() }

        assertEquals(
            listOf(KeyboardType.Text, KeyboardType.Number, KeyboardType.Password, KeyboardType.Email),
            composeKeyboardTypes
        )
    }
}
