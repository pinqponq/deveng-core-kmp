package core.presentation.component.datepicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import core.presentation.component.PickerField
import core.presentation.component.RoundedSurface
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import core.util.datetime.CustomSelectableDates
import core.util.datetime.getMonthNames
import core.util.datetime.TargetDates
import core.util.datetime.toEpochMillis
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.date_picker
import global.deveng.deveng_core.generated.resources.shared_day
import global.deveng.deveng_core.generated.resources.shared_month
import global.deveng.deveng_core.generated.resources.shared_cancel
import global.deveng.deveng_core.generated.resources.shared_ic_calendar
import global.deveng.deveng_core.generated.resources.shared_ok
import global.deveng.deveng_core.generated.resources.shared_year
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.abs

private const val DEFAULT_INITIAL_YEAR_OFFSET = 18

/**
 * A customizable wheel-style date picker that opens in a dialog.
 *
 * Uses three independent scrolling columns (day, month, year) and allows users
 * to customize colors and text styles either via ComponentTheme or per-parameter overrides.
 *
 * @param modifier Modifier to be applied to the picker field container.
 * @param trailingIconModifier Modifier to be applied to the trailing calendar icon.
 * @param isEnabled Whether the field is enabled and can be clicked. Default is true.
 * @param title Title text displayed above the picker field.
 * @param selectedDate Currently selected date, or null if none selected.
 * @param onDateChange Callback invoked when user confirms a date.
 * @param selectedDateText Optional formatted text shown in picker field. Falls back to dd/MM/yyyy.
 * @param placeholderText Placeholder shown when no date is selected.
 * @param errorMessage Optional error message displayed below the picker field.
 * @param targetDates Date target mode for wheel range. PAST for birth-date style, FUTURE for reservation style.
 * @param pastStartYear Oldest year used for PAST target range. Default is 1900.
 * @param pastEndYear Newest year used for PAST target range. If null, the current year is used.
 * @param initialYear Year the wheel opens on when [selectedDate] is null. If null, the current year
 * minus 18 is used. Values outside the year range are clamped to it.
 * @param futureYearSpan Number of years added to currentYear for FUTURE target range. Default is 120.
 * @param dayLabel Optional label text for day wheel.
 * @param monthLabel Optional label text for month wheel.
 * @param yearLabel Optional label text for year wheel.
 * @param isLabelsVisible Whether labels are visible below columns.
 * @param monthNames Optional month display names for month wheel. Must contain 12 items if provided.
 * @param selectableDates CustomSelectableDates instance to configure date restrictions. If null,
 * a new instance will be created and targetDates will be applied.
 * @param confirmButtonText Dialog confirm button text.
 * @param dismissButtonText Dialog dismiss button text.
 * @param trailingIconTint Color tint for trailing icon. If null, uses theme default.
 * @param dialogContainerColor Dialog container color. If null, uses theme default.
 * @param dialogContentColor Dialog content color. If null, uses theme default.
 * @param selectedItemTextStyle Text style for centered (selected) wheel item. If null, uses theme default.
 * @param unselectedItemTextStyle Text style for unselected wheel items. If null, uses theme default.
 * @param labelTextStyle Text style for day/month/year labels. If null, uses theme default.
 * @param selectorLineColor Color for selector lines around center row. If null, uses theme default.
 * @param confirmButtonTextColor Color for confirm button text. If null, uses theme default.
 * @param dismissButtonTextColor Color for dismiss button text. If null, uses theme default.
 */
@Composable
fun WheelDatePicker(
    modifier: Modifier = Modifier,
    trailingIconModifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    title: String,
    selectedDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    selectedDateText: String? = null,
    placeholderText: String = "-",
    errorMessage: String? = null,
    targetDates: TargetDates = TargetDates.PAST,
    pastStartYear: Int = 1900,
    pastEndYear: Int? = null,
    initialYear: Int? = null,
    futureYearSpan: Int = 120,
    dayLabel: String = stringResource(Res.string.shared_day),
    monthLabel: String = stringResource(Res.string.shared_month),
    yearLabel: String = stringResource(Res.string.shared_year),
    isLabelsVisible: Boolean = true,
    monthNames: List<String>? = null,
    selectableDates: CustomSelectableDates? = null,
    confirmButtonText: String = stringResource(Res.string.shared_ok),
    dismissButtonText: String = stringResource(Res.string.shared_cancel),
    trailingIconTint: Color? = null,
    dialogContainerColor: Color? = null,
    dialogContentColor: Color? = null,
    selectedItemTextStyle: TextStyle? = null,
    unselectedItemTextStyle: TextStyle? = null,
    labelTextStyle: TextStyle? = null,
    selectorLineColor: Color? = null,
    confirmButtonTextColor: Color? = null,
    dismissButtonTextColor: Color? = null
) {
    val componentTheme = LocalComponentTheme.current
    val wheelDatePickerTheme = componentTheme.wheelDatePicker

    val finalTrailingIconTint = trailingIconTint ?: wheelDatePickerTheme.trailingIconTint
    val finalDialogContainerColor = dialogContainerColor ?: wheelDatePickerTheme.dialogContainerColor
    val finalDialogContentColor = dialogContentColor ?: wheelDatePickerTheme.dialogContentColor
    val finalSelectedItemTextStyle = (selectedItemTextStyle ?: wheelDatePickerTheme.selectedItemTextStyle)
        .let { style ->
            if (selectedItemTextStyle == null) style.copy(color = finalDialogContentColor) else style
        }
    val finalUnselectedItemTextStyle = (unselectedItemTextStyle
        ?: wheelDatePickerTheme.unselectedItemTextStyle)
        .let { style ->
            if (unselectedItemTextStyle == null) style.copy(color = finalDialogContentColor.copy(alpha = 0.65f))
            else style
        }
    val finalLabelTextStyle = (labelTextStyle ?: wheelDatePickerTheme.labelTextStyle)
        .let { style ->
            if (labelTextStyle == null) style.copy(color = finalDialogContentColor.copy(alpha = 0.8f))
            else style
        }
    val finalSelectorLineColor = selectorLineColor ?: wheelDatePickerTheme.selectorLineColor
    val finalConfirmButtonTextColor =
        confirmButtonTextColor ?: wheelDatePickerTheme.confirmButtonTextColor
    val finalDismissButtonTextColor =
        dismissButtonTextColor ?: wheelDatePickerTheme.dismissButtonTextColor

    val currentDate = remember {
        kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val currentYear = currentDate.year

    val finalSelectableDates = remember(selectableDates, targetDates) {
        selectableDates ?: CustomSelectableDates().apply {
            setTargetDates(targetDates)
        }
    }
    val normalizedFutureYearSpan = remember(futureYearSpan) {
        futureYearSpan.coerceAtLeast(0)
    }
    val yearRange = remember(targetDates, currentYear, pastStartYear, pastEndYear, normalizedFutureYearSpan) {
        when (targetDates) {
            TargetDates.PAST -> pastStartYear..(pastEndYear ?: currentYear)
            TargetDates.FUTURE -> currentYear..(currentYear + normalizedFutureYearSpan)
        }
    }
    val years = remember(yearRange, currentYear, finalSelectableDates) {
        yearRange
            .filter(finalSelectableDates::isSelectableYear)
            .ifEmpty { listOf(currentYear) }
    }
    val resolvedMonthNames = remember(monthNames, Locale.current.language) {
        val names = monthNames ?: getMonthNames(Locale.current.language)
        if (names.size == 12) names else getMonthNames("en")
    }
    val initialDate = remember(selectedDate, currentDate, initialYear, years) {
        selectedDate ?: currentDate.withYear(
            year = (initialYear ?: (currentYear - DEFAULT_INITIAL_YEAR_OFFSET)).coerceIn(years.first(), years.last())
        )
    }
    var showDialog by remember { mutableStateOf(false) }
    var selectedYear by remember(showDialog, initialDate) {
        mutableIntStateOf(initialDate.year.coerceIn(years.first(), years.last()))
    }
    var selectedMonth by remember(showDialog, initialDate) {
        mutableIntStateOf(initialDate.monthNumber)
    }
    var selectedDay by remember(showDialog, initialDate) {
        mutableIntStateOf(initialDate.dayOfMonth)
    }

    val maxDay = remember(selectedYear, selectedMonth) {
        daysInMonth(year = selectedYear, month = selectedMonth)
    }
    val selectableMonths = remember(selectedYear, finalSelectableDates, initialDate) {
        (1..12)
            .filter { month ->
                (1..daysInMonth(selectedYear, month)).any { day ->
                    finalSelectableDates.isSelectableDate(
                        LocalDate(selectedYear, month, day).toEpochMillis()
                    )
                }
            }
            .ifEmpty { listOf(initialDate.monthNumber.coerceIn(1, 12)) }
    }
    LaunchedEffect(selectableMonths) {
        if (selectedMonth !in selectableMonths) {
            selectedMonth = selectableMonths.first()
        }
    }

    val selectableDays = remember(selectedYear, selectedMonth, maxDay, finalSelectableDates) {
        (1..maxDay)
            .filter { day ->
                finalSelectableDates.isSelectableDate(
                    LocalDate(selectedYear, selectedMonth, day).toEpochMillis()
                )
            }
            .ifEmpty { listOf(1) }
    }
    LaunchedEffect(selectableDays) {
        if (selectedDay !in selectableDays) {
            selectedDay = selectableDays.first()
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            RoundedSurface(
                shape = RoundedCornerShape(16.dp),
                color = finalDialogContainerColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WheelDateColumn(
                            modifier = Modifier.weight(1f),
                            items = selectableDays,
                            selectedValue = selectedDay,
                            label = dayLabel,
                            isLabelVisible = isLabelsVisible,
                            selectedItemTextStyle = finalSelectedItemTextStyle,
                            unselectedItemTextStyle = finalUnselectedItemTextStyle,
                            labelTextStyle = finalLabelTextStyle,
                            selectorLineColor = finalSelectorLineColor,
                            onValueChange = { selectedDay = it }
                        )
                        WheelDateColumn(
                            modifier = Modifier.weight(1f),
                            items = selectableMonths,
                            selectedValue = selectedMonth,
                            label = monthLabel,
                            isLabelVisible = isLabelsVisible,
                            itemText = { month -> resolvedMonthNames[month - 1] },
                            selectedItemTextStyle = finalSelectedItemTextStyle,
                            unselectedItemTextStyle = finalUnselectedItemTextStyle,
                            labelTextStyle = finalLabelTextStyle,
                            selectorLineColor = finalSelectorLineColor,
                            onValueChange = { month ->
                                selectedMonth = month
                            }
                        )
                        WheelDateColumn(
                            modifier = Modifier.weight(1f),
                            items = years,
                            selectedValue = selectedYear,
                            label = yearLabel,
                            isLabelVisible = isLabelsVisible,
                            selectedItemTextStyle = finalSelectedItemTextStyle,
                            unselectedItemTextStyle = finalUnselectedItemTextStyle,
                            labelTextStyle = finalLabelTextStyle,
                            selectorLineColor = finalSelectorLineColor,
                            onValueChange = { selectedYear = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDialog = false }) {
                            Text(
                                text = dismissButtonText,
                                color = finalDismissButtonTextColor
                            )
                        }
                        TextButton(
                            onClick = {
                                onDateChange(LocalDate(selectedYear, selectedMonth, selectedDay))
                                showDialog = false
                            }
                        ) {
                            Text(
                                text = confirmButtonText,
                                color = finalConfirmButtonTextColor
                            )
                        }
                    }
                }
            }
        }
    }

    PickerField(
        modifier = modifier,
        isEnabled = isEnabled,
        title = title,
        text = selectedDateText ?: selectedDate?.toDisplayText(),
        hint = placeholderText,
        errorMessage = errorMessage,
        trailingSlot = {
            Icon(
                modifier = trailingIconModifier,
                painter = painterResource(Res.drawable.shared_ic_calendar),
                tint = finalTrailingIconTint,
                contentDescription = stringResource(Res.string.date_picker)
            )
        },
        onClick = {
            if (isEnabled) {
                showDialog = true
            }
        }
    )
}

@Composable
private fun WheelDateColumn(
    modifier: Modifier = Modifier,
    items: List<Int>,
    selectedValue: Int,
    label: String?,
    isLabelVisible: Boolean,
    itemText: (Int) -> String = { it.toString() },
    selectedItemTextStyle: TextStyle,
    unselectedItemTextStyle: TextStyle,
    labelTextStyle: TextStyle,
    selectorLineColor: Color,
    itemHeight: Dp = 36.dp,
    onValueChange: (Int) -> Unit
) {
    val selectedIndex = items.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(selectedIndex) {
        listState.scrollToItem(selectedIndex)
    }

    LaunchedEffect(listState, items, selectedValue) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (!isScrolling && items.isNotEmpty()) {
                    val layoutInfo = listState.layoutInfo
                    val viewportCenter =
                        (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                        abs((itemInfo.offset + itemInfo.size / 2) - viewportCenter)
                    } ?: return@collect
                    val roundedIndex = centeredItem.index.coerceIn(0, items.lastIndex)

                    if (items[roundedIndex] != selectedValue) {
                        onValueChange(items[roundedIndex])
                    }
                    if (listState.firstVisibleItemIndex != roundedIndex || listState.firstVisibleItemScrollOffset != 0) {
                        listState.animateScrollToItem(roundedIndex)
                    }
                }
            }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 5)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = itemHeight * 2)
            ) {
                itemsIndexed(items) { index, value ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        val isSelected = index == selectedIndex
                        val baseStyle =
                            if (isSelected) selectedItemTextStyle else unselectedItemTextStyle
                        Text(
                            text = itemText(value),
                            style = baseStyle,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = -itemHeight / 2),
                color = selectorLineColor
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = itemHeight / 2),
                color = selectorLineColor
            )
        }

        if (isLabelVisible && !label.isNullOrBlank()) {
            Text(
                text = label,
                style = labelTextStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun LocalDate.toDisplayText(): String {
    val day = day.toString().padStart(2, '0')
    val month = month.number.toString().padStart(2, '0')
    return "$day/$month/$year"
}

private fun LocalDate.withYear(year: Int): LocalDate {
    return LocalDate(year, monthNumber, dayOfMonth.coerceAtMost(daysInMonth(year = year, month = monthNumber)))
}

private fun daysInMonth(year: Int, month: Int): Int {
    return (31 downTo 28).firstOrNull { day ->
        runCatching { LocalDate(year, month, day) }.isSuccess
    } ?: 30
}

@Preview
@Composable
private fun WheelDatePickerPreview() {
    var selectedDate by remember { mutableStateOf(LocalDate(2026, 4, 28)) }

    AppTheme {
        WheelDatePicker(
            title = "Date",
            selectedDate = selectedDate,
            onDateChange = { selectedDate = it }
        )
    }
}
