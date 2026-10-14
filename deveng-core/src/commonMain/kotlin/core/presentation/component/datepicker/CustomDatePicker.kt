package core.presentation.component.datepicker

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import core.presentation.component.PickerField
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import core.util.datetime.CustomSelectableDates
import core.util.datetime.TargetDates
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.date_picker
import global.deveng.deveng_core.generated.resources.shared_cancel
import global.deveng.deveng_core.generated.resources.shared_ic_calendar
import global.deveng.deveng_core.generated.resources.shared_ok
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A customizable date picker component that displays a picker field and opens a date picker dialog.
 * Supports selecting a single date with customizable date range restrictions.
 *
 * @param modifier Modifier to be applied to the picker field container.
 * @param trailingIconModifier Modifier to be applied to the trailing calendar icon.,
 * @param isEnabled Whether the field is enabled and can be clicked. Default is true.
 * @param selectedDate Currently selected date, or null if none selected.
 * @param onDateChange Callback invoked when a date is selected, receives the selected LocalDate.
 * @param targetDates Target date range restriction (PAST, FUTURE, or ALL). Default is PAST.
 * @param title Title text displayed above the picker field.
 * @param placeholderText Placeholder text shown when no date is selected. Default is "-".
 * @param selectedDateText Formatted text representation of the selected date to display.
 * @param errorMessage Optional error message displayed below the picker field.
 * @param selectableDates CustomSelectableDates instance to configure date selection restrictions. If null, a new instance will be created with the specified targetDates.
 * @param trailingIconTint Color tint for the trailing calendar icon. If null, uses theme default.
 * @param dialogContainerColor Background color of the date picker dialog. If null, uses theme default.
 * @param dialogContentColor Text color for dialog content. If null, uses theme default.
 * @param selectedDayContainerColor Background color of the selected day in the calendar. If null, uses theme default.
 * @param selectedDayContentColor Text color of the selected day. If null, uses theme default.
 * @param selectedYearContainerColor Background color of the selected year. If null, uses theme default.
 * @param selectedYearContentColor Text color of the selected year. If null, uses theme default.
 * @param todayContentColor Text color for today's date. If null, uses theme default.
 * @param todayDateBorderColor Border color for today's date. If null, uses theme default.
 * @param confirmButtonTextColor Text color of the confirm button. If null, uses theme default.
 * @param dismissButtonTextColor Text color of the dismiss button. If null, uses theme default.
 * @param leadingSlot Optional composable slot for leading content in the picker field.
 * @param trailingSlot Optional composable slot for trailing content (replaces default calendar icon if provided).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun CustomDatePicker(
    modifier: Modifier = Modifier,
    trailingIconModifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    selectedDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    targetDates: TargetDates = TargetDates.PAST,
    title: String,
    placeholderText: String = "-",
    selectedDateText: String?,
    errorMessage: String? = null,
    selectableDates: CustomSelectableDates? = null,
    trailingIconTint: Color? = null,
    dialogContainerColor: Color? = null,
    dialogContentColor: Color? = null,
    selectedDayContainerColor: Color? = null,
    selectedDayContentColor: Color? = null,
    selectedYearContainerColor: Color? = null,
    selectedYearContentColor: Color? = null,
    todayContentColor: Color? = null,
    todayDateBorderColor: Color? = null,
    confirmButtonTextColor: Color? = null,
    dismissButtonTextColor: Color? = null,
    leadingSlot: @Composable (() -> Unit)? = null,
    trailingSlot: @Composable (() -> Unit)? = null
) {
    val componentTheme = LocalComponentTheme.current
    val datePickerTheme = componentTheme.datePicker

    val finalTrailingIconTint = trailingIconTint ?: datePickerTheme.trailingIconTint
    val finalDialogContainerColor =
        dialogContainerColor ?: datePickerTheme.dialogContainerColor
    val finalDialogContentColor =
        dialogContentColor ?: datePickerTheme.dialogContentColor
    val finalSelectedDayContainerColor =
        selectedDayContainerColor ?: datePickerTheme.selectedDayContainerColor
    val finalSelectedDayContentColor =
        selectedDayContentColor ?: datePickerTheme.selectedDayContentColor
    val finalSelectedYearContainerColor =
        selectedYearContainerColor ?: datePickerTheme.selectedYearContainerColor
    val finalSelectedYearContentColor =
        selectedYearContentColor ?: datePickerTheme.selectedYearContentColor
    val finalTodayContentColor = todayContentColor ?: datePickerTheme.todayContentColor
    val finalTodayDateBorderColor =
        todayDateBorderColor ?: datePickerTheme.todayDateBorderColor
    val finalConfirmButtonColor =
        confirmButtonTextColor ?: datePickerTheme.confirmButtonTextColor
    val finalDismissButtonColor =
        dismissButtonTextColor ?: datePickerTheme.dismissButtonTextColor

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        // Material's DatePicker reads and returns the selected day as UTC midnight milliseconds.
        val startOfDayInstant = selectedDate?.atStartOfDayIn(TimeZone.UTC)
        val epochMilliseconds = startOfDayInstant?.toEpochMilliseconds()

        val finalSelectableDates = selectableDates ?: CustomSelectableDates().apply {
            setTargetDates(targetDates)
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = epochMilliseconds,
            selectableDates = finalSelectableDates
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        onDateChange(newDate)
                    }
                    showDialog = false
                }) {
                    Text(
                        text = stringResource(Res.string.shared_ok),
                        color = finalConfirmButtonColor
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(
                        text = stringResource(Res.string.shared_cancel),
                        color = finalDismissButtonColor
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = finalDialogContainerColor
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = finalDialogContainerColor,
                    titleContentColor = finalDialogContentColor,
                    headlineContentColor = finalDialogContentColor,
                    weekdayContentColor = finalDialogContentColor,
                    subheadContentColor = finalDialogContentColor,
                    navigationContentColor = finalDialogContentColor,
                    dayContentColor = finalDialogContentColor,
                    selectedDayContentColor = finalSelectedDayContentColor,
                    selectedDayContainerColor = finalSelectedDayContainerColor,
                    todayContentColor = finalTodayContentColor,
                    todayDateBorderColor = finalTodayDateBorderColor,
                    yearContentColor = finalDialogContentColor,
                    currentYearContentColor = finalDialogContentColor,
                    selectedYearContentColor = finalSelectedYearContentColor,
                    selectedYearContainerColor = finalSelectedYearContainerColor,
                    dividerColor = finalDialogContentColor
                )
            )
        }
    }

    val defaultTrailingSlot: @Composable () -> Unit = {
        Icon(
            modifier = trailingIconModifier,
            painter = painterResource(Res.drawable.shared_ic_calendar),
            tint = finalTrailingIconTint,
            contentDescription = stringResource(Res.string.date_picker)
        )
    }

    PickerField(
        modifier = modifier,
        text = selectedDateText,
        hint = placeholderText,
        title = title,
        leadingSlot = leadingSlot,
        trailingSlot = trailingSlot ?: defaultTrailingSlot,
        errorMessage = errorMessage,
        onClick = {
            if (isEnabled) {
                showDialog = true
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CustomDatePickerPreview() {
    AppTheme {
        CustomDatePicker(
            selectedDate = LocalDate.parse("2023-01-01"),
            onDateChange = {},
            title = "takvim",
            selectedDateText = "01/01/2023"
        )
    }
}