package core.presentation.figma

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.figma.code.connect.FigmaConnect
import core.presentation.component.datepicker.CustomDatePicker
import core.presentation.theme.CoreCustomBlackColor
import core.util.datetime.CustomSelectableDates
import core.util.datetime.TargetDates
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.date_picker
import global.deveng.deveng_core.generated.resources.shared_ic_calendar
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@FigmaConnect(
    url = "https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=36-39&m=dev"
)
class CustomDatePickerDoc {
    // --- INTERNAL ENUMS MIRRORING FIGMA VARIANTS / PROPS ---

    enum class TargetDatesVariant { Past, Future }

    enum class SlotPresence { None, Leading, Trailing, Both }

    // --- FIGMA-PROPERTIES (MAPPED TO FIGMA COMPONENT PROPS) ---

    val selectedDateText: String? = null
    // Formatted text representation of the selected date to display in the picker field.
    // Expected format: ISO 8601 date string (e.g., "2023-01-15") or custom formatted string.
    // If null, placeholderText is shown instead.

    val title: String = "Select date"

    val placeholderText: String = "-"

    val errorMessage: String? = null

    val targetDatesVariant: TargetDatesVariant = TargetDatesVariant.Future
    // Restricts selectable date range in the picker dialog.
    // Past: Only dates before today are selectable.
    // Future: Only dates after today are selectable.

    val slotPresence: SlotPresence = SlotPresence.None

    // --- DERIVED VALUES (NOT DIRECTLY FIGMA PROPS, BUT NEEDED FOR FULL API) ---

    // 1. modifier
    val modifier: Modifier = Modifier

    // 2. trailingIconModifier
    val trailingIconModifier: Modifier = Modifier

    // 3. selectedDate (parsed from selectedDateText if provided)
    // Parses ISO 8601 date string (YYYY-MM-DD format) into LocalDate.
    // Returns null if parsing fails or selectedDateText is null.
    val selectedDate: LocalDate?
        get() = selectedDateText?.let {
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                null
            }
        }

    // 4. targetDates
    // Maps enum variant to TargetDates constant that controls date selection restrictions.
    val targetDates: TargetDates
        get() = when (targetDatesVariant) {
            TargetDatesVariant.Past -> TargetDates.PAST
            TargetDatesVariant.Future -> TargetDates.FUTURE
        }

    // 5. slots
    // Leading slot: Optional icon displayed at the start of the picker field.
    val leadingSlot: @Composable (() -> Unit)?
        get() = when (slotPresence) {
            SlotPresence.Leading, SlotPresence.Both -> {
                {
                    Icon(
                        painter = painterResource(Res.drawable.shared_ic_calendar),
                        contentDescription = null
                    )
                }
            }
            else -> null
        }

    // 6. colors (null = use theme defaults)
    val trailingIconTint: Color? = null

    // Trailing slot: Icon displayed at the end of the picker field.
    // Default: Calendar icon (shared_ic_calendar) is shown when no custom trailing slot is provided.
    // When slotPresence is Trailing or Both, a custom icon is used; otherwise, the default calendar icon is shown.
    val trailingSlot: @Composable (() -> Unit)?
        get() = when (slotPresence) {
            SlotPresence.Trailing, SlotPresence.Both -> {
                {
                    Icon(
                        modifier = trailingIconModifier,
                        painter = painterResource(Res.drawable.shared_ic_calendar),
                        tint = trailingIconTint ?: CoreCustomBlackColor,
                        contentDescription = stringResource(Res.string.date_picker)
                    )
                }
            }
            else -> null // Component provides default calendar icon when null
        }
    val dialogContainerColor: Color? = null
    val dialogContentColor: Color? = null
    val selectedDayContainerColor: Color? = null
    val selectedDayContentColor: Color? = null
    val selectedYearContainerColor: Color? = null
    val selectedYearContentColor: Color? = null
    val todayContentColor: Color? = null
    val todayDateBorderColor: Color? = null
    val confirmButtonTextColor: Color? = null
    val dismissButtonTextColor: Color? = null

    // 7. Behavior notes
    // - Clicking the picker field opens a date picker dialog
    // - Dialog shows a calendar interface with date selection
    // - User confirms selection via "OK" button or dismisses via "Cancel" button
    // - selectableDates is required but created internally using remember() - no need to provide externally

    // 8. callback
    // onDateChange: Invoked when user confirms a date selection in the dialog.
    // Receives the selected LocalDate object.
    val onDateChange: (LocalDate) -> Unit = {}

    // --- THE COMPOSABLE SNIPPET (USES *ALL* PARAMETERS EXPLICITLY) ---

    @Composable
    fun Component() {
        // selectableDates is required by the component but created internally.
        // It's remembered across recompositions and configured based on targetDates.
        val selectableDates = remember { CustomSelectableDates() }

        CustomDatePicker(
            modifier = modifier,
            trailingIconModifier = trailingIconModifier,
            selectedDate = selectedDate,
            onDateChange = onDateChange,
            targetDates = targetDates,
            title = title,
            placeholderText = placeholderText,
            selectedDateText = selectedDateText,
            errorMessage = errorMessage,
            selectableDates = selectableDates,
            trailingIconTint = trailingIconTint,
            dialogContainerColor = dialogContainerColor,
            dialogContentColor = dialogContentColor,
            selectedDayContainerColor = selectedDayContainerColor,
            selectedDayContentColor = selectedDayContentColor,
            selectedYearContainerColor = selectedYearContainerColor,
            selectedYearContentColor = selectedYearContentColor,
            todayContentColor = todayContentColor,
            todayDateBorderColor = todayDateBorderColor,
            confirmButtonTextColor = confirmButtonTextColor,
            dismissButtonTextColor = dismissButtonTextColor,
            leadingSlot = leadingSlot,
            trailingSlot = trailingSlot
        )
    }
}
