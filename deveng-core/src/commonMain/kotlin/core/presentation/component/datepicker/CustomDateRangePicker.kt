package core.presentation.component.datepicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import core.presentation.component.PickerField
import core.presentation.theme.LocalComponentTheme
import core.util.datetime.CustomSelectableDates
import core.util.datetime.TargetDates
import core.util.datetime.toEpochMillis
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.date_picker
import global.deveng.deveng_core.generated.resources.shared_cancel
import global.deveng.deveng_core.generated.resources.shared_ic_calendar
import global.deveng.deveng_core.generated.resources.shared_ok
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A customizable date range picker component that displays a picker field and opens a date range picker dialog.
 * Supports selecting a start and end date with customizable date range restrictions.
 *
 * @param modifier Modifier to be applied to the picker field container.
 * @param title Optional title text displayed above the picker field.
 * @param titleColor Color of the title text. If null, uses theme default.
 * @param shape Shape of the picker field container. If null, uses theme default.
 * @param enabledBorderWidth Width of the border when enabled. If null, uses theme default.
 * @param enabledBorderColor Border color when enabled. If null, uses theme default.
 * @param enabledTextColor Text color when enabled. If null, uses theme default.
 * @param rangeText Formatted text representation of the selected date range to display.
 * @param initialSelectedStartDate Initially selected start date, or null if none.
 * @param initialSelectedEndDate Initially selected end date, or null if none.
 * @param trailingIconModifier Modifier to be applied to the trailing calendar icon.
 * @param trailingIconTint Color tint for the trailing calendar icon. If null, uses theme default.
 * @param errorMessage Optional error message displayed below the picker field.
 * @param onRangeChange Callback invoked when a date range is selected, receives start and end dates (both nullable).
 * @param targetDates Target date range restriction (PAST, FUTURE, or ALL). Default is FUTURE.
 * @param selectableDates CustomSelectableDates instance to configure date selection restrictions. If null, a new instance will be created with the specified targetDates.
 * @param dialogContainerColor Background color of the date picker dialog. If null, uses theme default.
 * @param dialogContentColor Default text color for dialog content. If null, uses theme default.
 * @param titleContentColor Text color for the dialog title. If null, uses theme default.
 * @param headlineContentColor Text color for the headline. If null, uses theme default.
 * @param headlineTextStyle Text style for the headline. If null, uses theme default.
 * @param weekdayContentColor Text color for weekday labels. If null, uses theme default.
 * @param subheadContentColor Text color for subhead content. If null, uses theme default.
 * @param navigationContentColor Text color for navigation controls. If null, uses theme default.
 * @param dayContentColor Text color for day numbers. If null, uses theme default.
 * @param selectedDayContainerColor Background color of selected days. If null, uses theme default.
 * @param selectedDayContentColor Text color of selected days. If null, uses theme default.
 * @param todayContentColor Text color for today's date. If null, uses theme default.
 * @param todayDateBorderColor Border color for today's date. If null, uses theme default.
 * @param yearContentColor Text color for year numbers. If null, uses theme default.
 * @param currentYearContentColor Text color for the current year. If null, uses theme default.
 * @param selectedYearContainerColor Background color of the selected year. If null, uses theme default.
 * @param selectedYearContentColor Text color of the selected year. If null, uses theme default.
 * @param dividerColor Color of dividers in the dialog. If null, uses theme default.
 * @param confirmButtonTextColor Text color of the confirm button. If null, uses theme default.
 * @param dismissButtonTextColor Text color of the dismiss button. If null, uses theme default.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun CustomDateRangePicker(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color? = null,
    shape: CornerBasedShape? = null,
    enabledBorderWidth: Dp? = null,
    enabledBorderColor: Color? = null,
    enabledTextColor: Color? = null,
    rangeText: String,
    placeholderText: String = "-",
    initialSelectedStartDate: LocalDate?,
    initialSelectedEndDate: LocalDate?,
    trailingIconModifier: Modifier = Modifier,
    trailingIconTint: Color? = null,
    errorMessage: String? = null,
    onRangeChange: (LocalDate?, LocalDate?) -> Unit,
    targetDates: TargetDates = TargetDates.FUTURE,
    selectableDates: CustomSelectableDates? = null,
    dialogContainerColor: Color? = null,
    dialogContentColor: Color? = null,
    titleContentColor: Color? = null,
    headlineContentColor: Color? = null,
    headlineTextStyle: TextStyle? = null,
    weekdayContentColor: Color? = null,
    subheadContentColor: Color? = null,
    navigationContentColor: Color? = null,
    dayContentColor: Color? = null,
    selectedDayContainerColor: Color? = null,
    selectedDayContentColor: Color? = null,
    todayContentColor: Color? = null,
    todayDateBorderColor: Color? = null,
    yearContentColor: Color? = null,
    currentYearContentColor: Color? = null,
    selectedYearContainerColor: Color? = null,
    selectedYearContentColor: Color? = null,
    dividerColor: Color? = null,
    confirmButtonTextColor: Color? = null,
    dismissButtonTextColor: Color? = null
) {
    val componentTheme = LocalComponentTheme.current
    val dateRangePickerTheme = componentTheme.dateRangePicker
    val pickerFieldTheme = componentTheme.pickerField

    // PickerField theme values
    val finalTitleColor = titleColor ?: pickerFieldTheme.titleTextStyle.color
    val finalEnabledBorderWidth = enabledBorderWidth ?: pickerFieldTheme.enabledBorderWidth
    val finalEnabledBorderColor = enabledBorderColor ?: pickerFieldTheme.enabledBorderColor
    val finalEnabledTextColor = enabledTextColor ?: pickerFieldTheme.enabledTextColor
    val finalTrailingIconTint = trailingIconTint ?: dateRangePickerTheme.trailingIconTint

    // Dialog theme values
    val finalDialogContainerColor =
        dialogContainerColor ?: dateRangePickerTheme.dialogContainerColor
    val finalDialogContentColor = dialogContentColor ?: dateRangePickerTheme.dialogContentColor
    val finalTitleContentColor = titleContentColor ?: dateRangePickerTheme.titleContentColor
    val finalHeadlineContentColor =
        headlineContentColor ?: dateRangePickerTheme.headlineContentColor
    val finalHeadlineTextStyle = headlineTextStyle ?: dateRangePickerTheme.headlineTextStyle.copy(
        color = finalHeadlineContentColor
    )
    val finalWeekdayContentColor = weekdayContentColor ?: dateRangePickerTheme.weekdayContentColor
    val finalSubheadContentColor = subheadContentColor ?: dateRangePickerTheme.subheadContentColor
    val finalNavigationContentColor =
        navigationContentColor ?: dateRangePickerTheme.navigationContentColor
    val finalDayContentColor = dayContentColor ?: dateRangePickerTheme.dayContentColor
    val finalSelectedDayContainerColor =
        selectedDayContainerColor ?: dateRangePickerTheme.selectedDayContainerColor
    val finalSelectedDayContentColor =
        selectedDayContentColor ?: dateRangePickerTheme.selectedDayContentColor
    val finalTodayContentColor = todayContentColor ?: dateRangePickerTheme.todayContentColor
    val finalTodayDateBorderColor =
        todayDateBorderColor ?: dateRangePickerTheme.todayDateBorderColor
    val finalYearContentColor = yearContentColor ?: dateRangePickerTheme.yearContentColor
    val finalCurrentYearContentColor =
        currentYearContentColor ?: dateRangePickerTheme.currentYearContentColor
    val finalSelectedYearContainerColor =
        selectedYearContainerColor ?: dateRangePickerTheme.selectedYearContainerColor
    val finalSelectedYearContentColor =
        selectedYearContentColor ?: dateRangePickerTheme.selectedYearContentColor
    val finalDividerColor = dividerColor ?: dateRangePickerTheme.dividerColor
    val finalConfirmButtonTextColor =
        confirmButtonTextColor ?: dateRangePickerTheme.confirmButtonTextColor
    val finalDismissButtonTextColor =
        dismissButtonTextColor ?: dateRangePickerTheme.dismissButtonTextColor

    var showDialog by remember { mutableStateOf(false) }

    val localTimeZone = TimeZone.currentSystemDefault()

    if (showDialog) {
        // Create selectableDates instance if not provided
        val finalSelectableDates = selectableDates ?: CustomSelectableDates().apply {
            setTargetDates(targetDates)
        }

        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialSelectedStartDate?.toEpochMillis(),
            initialSelectedEndDateMillis = initialSelectedEndDate?.toEpochMillis(),
            selectableDates = finalSelectableDates
        )

        val startDate = state.selectedStartDateMillis?.let {
            Instant.fromEpochMilliseconds(it).toLocalDateTime(localTimeZone).date
        }

        val endDate = state.selectedEndDateMillis?.let {
            Instant.fromEpochMilliseconds(it).toLocalDateTime(localTimeZone).date
        }

        val pickerFormatter = remember {
            DatePickerDefaults.dateFormatter(
                selectedDateSkeleton = "EMMMd",
                selectedDateDescriptionSkeleton = "yMMMMEEEEd"
            )
        }

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRangeChange(startDate, endDate)
                        showDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.shared_ok),
                        color = finalConfirmButtonTextColor
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(
                        text = stringResource(Res.string.shared_cancel),
                        color = finalDismissButtonTextColor
                    )
                }
            },
            colors = DatePickerDefaults.colors(containerColor = finalDialogContainerColor)
        ) {
            DateRangePicker(
                state = state,
                title = null,
                dateFormatter = pickerFormatter,
                headline = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProvideTextStyle(finalHeadlineTextStyle) {
                            DateRangePickerDefaults.DateRangePickerHeadline(
                                selectedStartDateMillis = state.selectedStartDateMillis,
                                selectedEndDateMillis = state.selectedEndDateMillis,
                                displayMode = state.displayMode,
                                dateFormatter = pickerFormatter
                            )
                        }
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = finalDialogContainerColor,
                    titleContentColor = finalTitleContentColor,
                    headlineContentColor = finalHeadlineContentColor,
                    weekdayContentColor = finalWeekdayContentColor,
                    subheadContentColor = finalSubheadContentColor,
                    navigationContentColor = finalNavigationContentColor,
                    dayContentColor = finalDayContentColor,
                    selectedDayContentColor = finalSelectedDayContentColor,
                    selectedDayContainerColor = finalSelectedDayContainerColor,
                    todayContentColor = finalTodayContentColor,
                    todayDateBorderColor = finalTodayDateBorderColor,
                    yearContentColor = finalYearContentColor,
                    currentYearContentColor = finalCurrentYearContentColor,
                    selectedYearContentColor = finalSelectedYearContentColor,
                    selectedYearContainerColor = finalSelectedYearContainerColor,
                    dividerColor = finalDividerColor
                )
            )
        }
    }

    PickerField(
        modifier = modifier,
        title = title,
        titleColor = finalTitleColor,
        text = rangeText,
        enabledTextColor = finalEnabledTextColor,
        shape = shape,
        enabledBorderWidth = finalEnabledBorderWidth,
        enabledBorderColor = finalEnabledBorderColor,
        hint = placeholderText,
        trailingSlot = {
            Icon(
                modifier = trailingIconModifier,
                painter = painterResource(Res.drawable.shared_ic_calendar),
                tint = finalTrailingIconTint,
                contentDescription = stringResource(Res.string.date_picker)
            )
        },
        onClick = { showDialog = true },
        errorMessage = errorMessage
    )
}
