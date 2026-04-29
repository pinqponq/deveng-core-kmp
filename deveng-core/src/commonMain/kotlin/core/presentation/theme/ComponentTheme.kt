package core.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import org.jetbrains.compose.resources.DrawableResource

private val QrCodeViewDefaultShapes = QrShapes()

/**
 * Theme colors for CustomButton component
 */
data class ButtonTheme(
    val containerColor: Color = CoreSecondaryColor,
    val contentColor: Color = CoreOnPrimaryColor,
    val disabledContainerColor: Color = CoreSecondaryColor.copy(alpha = 0.4f),
    val disabledContentColor: Color = CoreOnPrimaryColor.copy(alpha = 0.4f),
    val defaultTextStyle: TextStyle = TextStyle(
        fontSize = 18.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
)

/**
 * Theme colors for CustomAlertDialog component
 */
data class AlertDialogTheme(
    val headerColor: Color = Color.White,
    val bodyColor: Color = Color.White,
    val titleColor: Color = CoreCustomBlackColor,
    val descriptionColor: Color = CoreCustomBlackColor,
    val dividerColor: Color = CoreCustomDividerColor,
    val positiveButtonColor: Color = Color.White,
    val positiveButtonTextColor: Color = CoreCustomBlackColor,
    val negativeButtonColor: Color = Color.White,
    val negativeButtonTextColor: Color = CoreCustomBlackColor,
    val iconColor: Color = CoreAlertDialogIconColor,
    val titleTextStyle: TextStyle = TextStyle(fontSize = 16.sp),
    val descriptionTextStyle: TextStyle = TextStyle(fontSize = 16.sp),
    val buttonTextStyle: TextStyle = TextStyle(fontSize = 16.sp)
)

/**
 * Theme colors for RoundedSurface component
 */
data class SurfaceTheme(
    val defaultColor: Color = Color.White,
    val defaultContentColor: Color = Color.Black
)

/**
 * Theme colors for CustomDialogHeader component
 */
data class DialogHeaderTheme(
    val titleColor: Color = CoreCustomBlackColor,
    val iconTint: Color? = null,
    val titleTextStyle: TextStyle = TextStyle(fontSize = 16.sp)
)

/**
 * Theme for CustomIconButton component
 */
data class IconButtonTheme(
    val buttonSize: Dp = 54.dp,
    val backgroundColor: Color = CorePrimaryColor,
    val iconTint: Color = CoreCustomBlackColor,
    val shadowElevation: Dp = 0.dp,
    val shape: Shape = CircleShape
)

/**
 * Theme for SwipeCards overlay buttons (negative, revert, positive) and swipe-direction highlight colors.
 */
data class SwipeCardsTheme(
    val buttonBackgroundColor: Color = Color.White,
    val buttonIconTint: Color = CoreCustomBlackColor,
    val buttonSize: Dp = 40.dp,
    val buttonShadowElevation: Dp = 2.dp,
    val negativeSwipeHighlightColor: Color = Color(0xFFE57373),
    val positiveSwipeHighlightColor: Color = CorePrimaryColor,
    val swipeHighlightIconTintBlend: Float = 0.85f,
)

/**
 * Theme for ReviewStack component (stacked viewer with prev/next + negative/positive buttons).
 */
data class ReviewStackTheme(
    val cardCornerRadius: Dp = 16.dp,
    val cardColor: Color = Color(0xFF3A3A3A),
    val cardShadowElevation: Dp = 0.dp,
    val cardAspectRatio: Float = 1f,
    val stackScalePerLevel: Float = 0.05f,
    val stackTranslatePerLevel: Dp = 10.dp,
    val visibleStackDepth: Int = 3,
    val controlsSpacing: Dp = 16.dp,
    val controlsTopPadding: Dp = 12.dp,
    val controlsBottomPadding: Dp = 16.dp,
    val arrowButtonSize: Dp = 44.dp,
    val arrowButtonBackgroundColor: Color = Color.Transparent,
    val arrowButtonBorderColor: Color = CoreCustomGrayColor,
    val arrowButtonIconTint: Color = CorePrimaryColor,
    val arrowButtonShadowElevation: Dp = 0.dp,
    val decisionButtonSize: Dp = 44.dp,
    val decisionButtonShape: Shape = CircleShape,
    val decisionButtonBackgroundColor: Color = Color.Transparent,
    val negativeBorderColor: Color = CoreErrorColor,
    val negativeIconTint: Color = CoreErrorColor,
    val positiveBorderColor: Color = CorePrimaryColor,
    val positiveIconTint: Color = CorePrimaryColor,
    val decisionBorderWidth: Dp = 1.dp,
    val decisionContentSpacing: Dp = 6.dp,
    val decisionCountTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
    ),
    val negativeCountColor: Color = CoreErrorColor,
    val positiveCountColor: Color = CorePrimaryColor,
    val indexIndicatorTextStyle: TextStyle = TextStyle(
        fontSize = 14.sp,
        color = CoreCustomGrayHintColor,
    ),
    val topBarPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    val positiveOverlayColor: Color = Color(0xFF2ECC71),
    val negativeOverlayColor: Color = Color(0xFFE74C3C),
    val overlayAlpha: Float = 0.35f,
    val exitScale: Float = 0.15f,
    val exitRotationDegrees: Float = 12f,
    val decisionAnimationPhase1Ms: Int = 160,
    val decisionAnimationPhase2Ms: Int = 220,
    /** Custom undo banner at the bottom of the card stack; auto-dismiss after this duration (ms). */
    val undoBannerVisibleMs: Int = 3000,
    val undoBannerHorizontalPadding: Dp = 16.dp,
    val undoBannerBottomPadding: Dp = 12.dp,
    val undoBannerCornerRadius: Dp = 8.dp,
    val undoBannerContainerColor: Color = Color(0xFF323232),
    val undoBannerContentColor: Color = Color.White,
    val undoBannerActionColor: Color = CorePrimaryColor,
    val undoBannerMessageTextStyle: TextStyle = TextStyle(fontSize = 14.sp),
    val undoBannerActionTextStyle: TextStyle = TextStyle(fontSize = 14.sp),
    /** When non-null, limits banner width inside the stack area. */
    val undoBannerMaxWidth: Dp? = null,
)

/**
 * Theme for LabeledSwitch component
 */
data class LabeledSwitchTheme(
    val labelTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomGrayHintColor
    ),
    val switchScale: Float = 0.75f,
    val checkedThumbColor: Color = Color.White,
    val checkedTrackColor: Color = CorePrimaryColor,
    val checkedBorderColor: Color = CorePrimaryColor,
    val uncheckedThumbColor: Color = CorePrimaryColor,
    val uncheckedTrackColor: Color = Color.White,
    val uncheckedBorderColor: Color = Color.White
)

/**
 * Theme for PickerField component
 */
data class PickerFieldTheme(
    val titleTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    val textStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    val hintTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomGrayHintColor
    ),
    val errorTextStyle: TextStyle = TextStyle(
        fontSize = 12.sp,
        color = Color(0xFFD32F2F)
    ),
    val shape: CornerBasedShape = RoundedCornerShape(12.dp),
    val enabledBackgroundColor: Color = Color.White,
    val enabledBorderColor: Color = Color.Transparent,
    val enabledBorderWidth: Dp = 0.dp,
    val enabledTextColor: Color = CoreCustomBlackColor,
    val hintTextColor: Color = CoreCustomGrayHintColor,
    val disabledBackgroundColor: Color = Color.Transparent,
    val disabledBorderColor: Color = Color.White,
    val disabledTextColor: Color = Color.White
)

/**
 * Theme for CustomDropDownMenu component
 */
data class CustomDropDownMenuTheme(
    val titleColor: Color = Color.White,
    val backgroundColor: Color = CoreCustomBlackColor,
    val dropDownMenuBackgroundColor: Color = CoreCustomBlackColor,
    val textColor: Color = Color.White,
    val hintTextColor: Color = CoreCustomGrayHintColor,
    val unfocusedBorderColor: Color = Color.Transparent,
    val focusedBorderColor: Color = CoreCustomGrayColor,
    val dividerColor: Color = CoreCustomGrayHintColor,
    val scrollBarColor: Color = CoreCustomGrayColor,
    val scrollBarTrackColor: Color = CoreCustomGrayHintColor,
    val isScrollBarVisible: Boolean = false,
    val shape: CornerBasedShape = RoundedCornerShape(12.dp),
    val fieldTextStyle: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    val menuItemTextStyle: TextStyle = TextStyle(
        fontSize = 14.sp
    )
)

/**
 * Theme for CustomTextField component
 */
data class CustomTextFieldTheme(
    val titleTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    val charCountTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomGrayHintColor
    ),
    val textStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    val hintTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomGrayHintColor
    ),
    val errorTextStyle: TextStyle = TextStyle(
        fontSize = 12.sp,
        color = Color(0xFFD32F2F)
    ),
    val containerShape: CornerBasedShape = RoundedCornerShape(12.dp),
    val borderStroke: BorderStroke = BorderStroke(0.dp, Color.Transparent),
    val containerColor: Color = Color.White,
    val disabledContainerColor: Color = Color.White,
    val textColor: Color = CoreCustomBlackColor,
    val disabledTextColor: Color = CoreCustomGrayHintColor,
    val readOnlyTextColor: Color = CoreCustomGrayHintColor,
    val cursorColor: Color = CoreCustomBlackColor,
    val isBorderActive: Boolean = true
)

/**
 * Theme for CustomDatePicker component
 */
data class DatePickerTheme(
    val trailingIconTint: Color = CoreCustomBlackColor,
    val dialogContainerColor: Color = CoreSecondaryColor,
    val dialogContentColor: Color = Color.White,
    val selectedDayContainerColor: Color = Color.White,
    val selectedDayContentColor: Color = CoreSecondaryColor,
    val selectedYearContainerColor: Color = Color.White,
    val selectedYearContentColor: Color = CoreSecondaryColor,
    val todayContentColor: Color = Color.White,
    val todayDateBorderColor: Color = Color.White,
    val confirmButtonTextColor: Color = Color.White,
    val dismissButtonTextColor: Color = Color.White
)

/**
 * Theme for CustomDateRangePicker component
 */
data class DateRangePickerTheme(
    val trailingIconTint: Color = CoreCustomBlackColor,
    val dialogContainerColor: Color = Color.White,
    val dialogContentColor: Color = CoreCustomBlackColor,
    val titleContentColor: Color = CorePrimaryColor,
    val headlineContentColor: Color = CoreCustomBlackColor,
    val headlineTextStyle: TextStyle = TextStyle(fontSize = 23.sp),
    val weekdayContentColor: Color = CoreCustomBlackColor,
    val subheadContentColor: Color = CoreCustomBlackColor,
    val navigationContentColor: Color = CoreCustomBlackColor,
    val dayContentColor: Color = CoreCustomBlackColor,
    val selectedDayContainerColor: Color = CorePrimaryColor,
    val selectedDayContentColor: Color = Color.White,
    val todayContentColor: Color = CorePrimaryColor,
    val todayDateBorderColor: Color = CorePrimaryColor,
    val yearContentColor: Color = CoreCustomBlackColor,
    val currentYearContentColor: Color = CoreCustomBlackColor,
    val selectedYearContainerColor: Color = CorePrimaryColor,
    val selectedYearContentColor: Color = Color.White,
    val dividerColor: Color = CoreCustomGrayColor,
    val confirmButtonTextColor: Color = CorePrimaryColor,
    val dismissButtonTextColor: Color = CorePrimaryColor
)

/**
 * Theme for WheelDatePicker component
 */
data class WheelDatePickerTheme(
    val trailingIconTint: Color = CoreCustomBlackColor,
    val dialogContainerColor: Color = Color.White,
    val dialogContentColor: Color = CoreCustomBlackColor,
    val selectedItemTextStyle: TextStyle = TextStyle(
        fontSize = 18.sp,
        color = CoreCustomBlackColor
    ),
    val unselectedItemTextStyle: TextStyle = TextStyle(
        fontSize = 14.sp,
        color = CoreCustomGrayHintColor
    ),
    val labelTextStyle: TextStyle = TextStyle(
        fontSize = 12.sp,
        color = CoreCustomGrayHintColor
    ),
    val selectorLineColor: Color = CoreCustomGrayHintColor.copy(alpha = 0.5f),
    val confirmButtonTextColor: Color = CorePrimaryColor,
    val dismissButtonTextColor: Color = CorePrimaryColor
)

/**
 * Theme for CustomHeader component
 */
data class HeaderTheme(
    val containerPadding: PaddingValues = PaddingValues(0.dp),
    val backgroundColor: Color = Color.Transparent,
    val leftIconTint: Color = Color.White,
    val rightIconTint: Color = Color.White,
    val leftIconBackgroundColor: Color = CorePrimaryColor,
    val rightIconBackgroundColor: Color = CorePrimaryColor,
    val iconButtonSize: Dp = 54.dp,
    val icon: DrawableResource? = null,
    val iconContentDescription: String? = null,
    val iconTint: Color = CoreCustomBlackColor,
    val iconSize: Dp = 24.dp
)

/**
 * Theme for OptionItem row
 */
data class OptionItemTheme(
    val backgroundColor: Color = Color.White,
    val horizontalPadding: Dp = 20.dp,
    val rowHeight: Dp = 50.dp,
    val textStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    val boldLeadingTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor,
        textDecoration = TextDecoration.Underline
    ),
    val leadingIconTint: Color = CoreCustomBlackColor,
    val trailingIconTint: Color = CoreCustomBlackColor,
    val checkIconTint: Color = CoreSecondaryColor,
    val checkIconBackgroundColor: Color = Color.White
)

/**
 * Theme for OptionItem list containers & dividers
 */
data class OptionItemListTheme(
    val containerColor: Color = Color.White,
    val dividerColor: Color = CoreCustomGrayHintColor,
    val containerShape: Shape = RoundedCornerShape(18.dp),
    val lazyListScrollbarColor: Color = CoreCustomGrayHintColor,
    val lazyListScrollbarWidth: Dp = 3.dp,
    val lazyListScrollbarTopPadding: Dp = 15.dp,
    val lazyListScrollbarBottomPadding: Dp = 15.dp,
    val optionItemBackgroundColor: Color = Color.White,
    val optionItemHorizontalPadding: Dp = 20.dp,
    val optionItemCheckIconTint: Color = CoreSecondaryColor,
    val optionItemTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    val saveButtonText: String? = null,
    val saveButtonTextStyle: TextStyle? = null,
    val saveButtonContainerColor: Color? = null,
    val saveButtonContentColor: Color? = null,
    val saveButtonShape: CornerBasedShape? = null
)

/**
 * Theme for ScrollbarWithScrollState component
 */
data class ScrollbarWithScrollStateTheme(
    val alwaysShowScrollBar: Boolean = false,
    val isScrollBarTrackVisible: Boolean = true,
    val scrollBarTrackColor: Color = Color.Gray,
    val scrollBarColor: Color = CoreCustomGrayHintColor,
    val scrollBarWidth: Dp = 5.dp,
    val scrollBarCornerRadius: Float = 4f,
    val scrollBarEndPadding: Float = 12f,
    val scrollBarTopPadding: Dp = 0.dp,
    val scrollBarBottomPadding: Dp = 0.dp
)

/**
 * Theme for ScrollbarWithLazyListState component
 */
data class ScrollbarWithLazyListStateTheme(
    val alwaysShowScrollBar: Boolean = false,
    val isScrollBarTrackVisible: Boolean = true,
    val scrollBarTrackColor: Color = Color.Gray,
    val scrollBarColor: Color = CorePrimaryColor,
    val scrollBarWidth: Dp = 5.dp,
    val scrollBarCornerRadius: Float = 4f,
    val scrollBarEndPadding: Float = 12f,
    val scrollBarTopPadding: Dp = 0.dp,
    val scrollBarBottomPadding: Dp = 0.dp
)

/**
 * Theme for SearchField component
 */
data class SearchFieldTheme(
    val buttonSize: Dp = 42.dp,
    val buttonShape: Shape = RoundedCornerShape(16.dp),
    val buttonBackgroundColor: Color = CorePrimaryColor,
    val buttonIconTint: Color = Color.White,
    val buttonShadowElevation: Dp = 0.dp,
    val defaultButtonIconDescription: String = "Search"
)

/**
 * Theme for ProgressIndicatorBars component
 */
data class ProgressIndicatorBarsTheme(
    val filledIndicatorColor: Color = CorePrimaryColor,
    val defaultIndicatorColor: Color = CoreCustomGrayHintColor,
    val indicatorHeight: Dp = 8.dp,
    val indicatorSpacing: Dp = 3.dp,
    val indicatorCornerRadius: Dp = 3.dp
)

/**
 * Theme for JsonViewer component
 */
data class JsonViewerTheme(
    val titleTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    val containerColor: Color = Color.White,
    val containerShape: CornerBasedShape = RoundedCornerShape(16.dp),
    val containerPadding: Dp = 20.dp,
    val jsonTextStyle: TextStyle = TextStyle(
        fontSize = 14.sp,
        color = CoreCustomBlackColor
    ),
    val buttonBackgroundColor: Color = CorePrimaryColor,
    val buttonTextColor: Color = Color.White,
    val buttonTextStyle: TextStyle = TextStyle(
        fontSize = 12.sp
    ),
    val buttonShape: CornerBasedShape = RoundedCornerShape(12.dp),
    val buttonHeight: Dp = 40.dp,
    val buttonIconSize: Dp = 15.dp,
    val copyIconTint: Color = Color.White,
    val copiedIconTint: Color = Color.White
)

/**
 * Theme for LabeledSlot component
 */
data class LabeledSlotTheme(
    val containerWidth: Dp = 112.dp,
    val containerHeight: Dp = 100.dp,
    val containerShape: CornerBasedShape = RoundedCornerShape(16.dp),
    val labelPadding: PaddingValues = PaddingValues(5.dp),
    val labelTextStyle: TextStyle = TextStyle(
        fontSize = 8.sp
    )
)

/**
 * Theme for NavigationMenu component
 */
data class NavigationMenuTheme(
    val expandedWidth: Dp = 256.dp,
    val collapsedWidth: Dp = 80.dp,
    val horizontalHeight: Dp = 80.dp,
    val backgroundColor: Color = CoreSecondaryColor,
    val shape: CornerBasedShape = RoundedCornerShape(30.dp),
    val verticalDividerColor: Color = CoreCustomGrayHintColor,
    val verticalDividerThickness: Dp = 1.dp,
    val verticalDividerTopBottomPadding: Dp = 25.dp,
    val sectionSeparatorColor: Color = CoreCustomGrayHintColor.copy(alpha = 0.2f),
    val horizontalItemSelectedBackgroundColor: Color = Color.Transparent,
    val verticalItemSelectedBackgroundColor: Color = Color.Transparent,
    val itemUnselectedBackgroundColor: Color = Color.Transparent,
    val collapsedHorizontalPadding: Dp = 16.dp,
    val collapsedVerticalPadding: Dp = 30.dp,
    val collapsedHeaderItemSpacing: Dp = 16.dp,
    val collapsedItemsSpacing: Dp = 10.dp,
    val collapsedItemSize: Dp = 48.dp,
    val collapsedItemCornerRadius: Dp = 8.dp,
    val collapsedItemIconSize: Dp = 20.dp,
    val expandedHorizontalPadding: Dp = 16.dp,
    val expandedVerticalPadding: Dp = 30.dp,
    val expandedHeaderItemSpacing: Dp = 16.dp,
    val expandedItemsSpacing: Dp = 10.dp,
    val expandedItemHeight: Dp = 48.dp,
    val expandedItemSpacedBy: Dp = 10.dp,
    val expandedItemCornerRadius: Dp = 20.dp,
    val expandedItemIconSize: Dp = 20.dp,
    val expandedItemStartPadding: Dp = 14.dp
)

/**
 * Theme for Chip item component
 */
data class ChipItemTheme(
    val selectedBackgroundColor: Color = CoreSecondaryColor,
    val unselectedBackgroundColor: Color = Color.Transparent,
    val selectedBorderStroke: BorderStroke = BorderStroke(
        width = 1.dp,
        color = CoreSecondaryColor
    ),
    val unselectedBorderStroke: BorderStroke = BorderStroke(
        width = 1.dp,
        color = CoreChipItemUnselectedBorderColor
    ),
    val shape: CornerBasedShape = RoundedCornerShape(59.dp),
    val horizontalPadding: Dp = 16.dp,
    val verticalPadding: Dp = 8.dp,
    val contentSpacing: Dp = 12.dp,
    val textStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    val selectedTextColor: Color = Color.White,
    val unselectedTextColor: Color = CoreCustomBlackColor,
    val leadingIconTint: Color? = null,
    val selectedLeadingIconTint: Color = Color.White,
    val unselectedLeadingIconTint: Color = CoreCustomBlackColor,
    val countSectionBackgroundColor: Color = CoreSecondaryColor,
    val countSectionTextColor: Color = Color.White,
    val countSectionTextStyle: TextStyle = TextStyle(
        fontSize = 12.sp,
        color = Color.White
    ),
    val countSectionHeight: Dp = 20.dp,
    val countSectionWidth: Dp = 29.dp,
    val countSectionShape: CornerBasedShape = RoundedCornerShape(8.dp)
)

/**
 * Theme for ChipsMenu component
 */
data class ChipsMenuTheme(
    val spaceBetweenItems: Dp = 10.dp,
    val startBrushWidth: Dp = 30.dp,
    val endBrushWidth: Dp = 30.dp
)

/**
 * Theme for TabRow component
 */
data class TabRowTheme(
    val tabHeight: Dp = 50.dp,
    val contentSpacing: Dp = 16.dp,
    val selectedTabBackgroundColor: Color = CoreSecondaryColor,
    val unselectedTabBackgroundColor: Color = CoreSecondaryColor,
    val selectedTabTitleTextStyle: TextStyle = TextStyle(
        fontSize = 14.sp,
        color = Color.White
    ),
    val unselectedTabTitleTextStyle: TextStyle = TextStyle(
        fontSize = 14.sp,
        color = Color.White
    ),
    val selectedTabNameColor: Color = Color.White,
    val unselectedTabNameColor: Color = Color.White,
    val selectedTabLineColor: Color = CoreCustomBlackColor,
    val unselectedTabLineColor: Color = CoreSecondaryColor,
    val selectedTabLineHeight: Dp = 3.dp,
    val unselectedTabLineHeight: Dp = 3.dp,
    val startBrushWidth: Dp = 30.dp,
    val endBrushWidth: Dp = 30.dp
)

/**
 * Theme for Shutter component
 */
data class ShutterTheme(
    val headerBackgroundColor: Color = CoreSecondaryColor,
    val headerInnerPadding: Dp = 10.dp,
    val headerContentSpacing: Dp = 3.dp,
    val headerShadowHeight: Dp = 8.dp,
    val headerShadowShape: Shape = RectangleShape,
    val titleTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = Color.White
    ),
    val descriptionTextStyle: TextStyle = TextStyle(
        fontSize = 14.sp,
        color = Color.White
    ),
    val expandIcon: DrawableResource? = null,
    val expandIconSize: Dp = 16.dp,
    val expandIconColor: Color = Color.White
)

/**
 * Theme for RatingRow component
 */
data class RatingRowTheme(
    val iconSize: Dp = 45.dp,
    val filledIcon: DrawableResource? = null,
    val filledIconTint: Color = CoreCustomAmberYellow,
    val unFilledIcon: DrawableResource? = null,
    val unFilledIconTint: Color = CoreCustomGrayHintColor,
    val iconDescription: String? = null,
    val animationScale: Float = 1.2f,
    val animationDampingRatio: Float = Spring.DampingRatioMediumBouncy,
    val animationStiffness: Float = 9000f
)

/**
 * Theme for CustomSlider component
 */
data class CustomSliderTheme(
    val titleTextStyle: TextStyle = TextStyle(),
    val currentValueTextStyle: TextStyle = TextStyle(),
    val errorMessageTextStyle: TextStyle = TextStyle(),
    val rangeLabelTextStyle: TextStyle = TextStyle(),
    val sliderRowSpacing: Dp = 8.dp,
    val errorMessageTopSpacing: Dp = 10.dp
)

/**
 * Theme for OtpView component
 */
data class OtpViewTheme(
    val textStyle: TextStyle = TextStyle(
        fontSize = 20.sp,
        textAlign = TextAlign.Center
    ),
    val shape: CornerBasedShape = RoundedCornerShape(8.dp),
    val digitWidth: Dp = 56.dp,
    val digitHeight: Dp = 50.dp,
    val digitSpacing: Dp = 11.dp,
    val borderWidth: Dp = 2.dp,
    val focusedBorderColor: Color = CoreSecondaryColor,
    val unfocusedBorderColor: Color = CoreCustomBlackColor,
    val errorBorderColor: Color = CoreErrorColor,
    val textColor: Color = CoreCustomBlackColor,
    val errorTextColor: Color = CoreErrorColor,
    val dotColor: Color = Color.Gray,
    val dotSize: Dp = 8.dp
)

/**
 * Theme for CustomQrCodeView component.
 *
 * @param darkColor Color used for the dark modules (the "data" pixels and finder squares).
 * @param lightColor Color used for the light modules and the surrounding background fill.
 * @param overlayFraction Fraction of the QR code's width/height that the centered
 *   `overlayContent` (typically a logo) is allowed to occupy, in `0f..1f`. The default
 *   `0.25f` covers ~25% of the code's area — paired with the default
 *   [io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel.High] (~30% recovery)
 *   this leaves a small safety margin so the code still scans reliably. Going much above
 *   ~0.30 risks unreadable codes even at level High; lower values (e.g. `0.20f`) are
 *   safer but shrink the logo. Ignored when no `overlayContent` is supplied.
 * @param pixelShape Shape used for the dark data modules (the "pixels"). Defaults to the
 *   library's square shape. Use the factories on [QrPixelShape.Companion] (e.g.
 *   `QrPixelShape.circle()`, `QrPixelShape.roundCorners()`) for a styled look.
 * @param ballShape Shape used for the inner square inside each of the three finder eyes.
 *   Defaults to the library's square shape. Factories on [QrBallShape.Companion].
 * @param frameShape Shape used for the outer ring of each of the three finder eyes.
 *   Defaults to the library's square shape. Factories on [QrFrameShape.Companion].
 */
data class QrCodeViewTheme(
    val darkColor: Color = Color.Black,
    val lightColor: Color = Color.White,
    val overlayFraction: Float = 0.25f,
    val pixelShape: QrPixelShape = QrCodeViewDefaultShapes.darkPixel,
    val ballShape: QrBallShape = QrCodeViewDefaultShapes.ball,
    val frameShape: QrFrameShape = QrCodeViewDefaultShapes.frame
)

/**
 * Typography theme for customizing font family across all components.
 *
 * @param fontFamily The default font family to use. If null, uses Urbanist font family.
 *                   Library users can provide their own FontFamily here.
 */
data class TypographyTheme(
    val fontFamily: FontFamily? = null
)

/**
 * Complete component theme containing all component-specific themes.
 *
 * Usage example:
 * ```
 * val customTheme = ComponentTheme(
 *     typography = TypographyTheme(
 *         fontFamily = FontFamily.SansSerif // Use system font
 *     ),
 *     button = ButtonTheme(
 *         containerColor = Color.Blue,
 *         contentColor = Color.White,
 *         defaultTextStyle = TextStyle(fontSize = 20.sp)
 *     ),
 *     alertDialog = AlertDialogTheme(
 *         headerColor = Color.LightGray,
 *         titleColor = Color.Black
 *     )
 * )
 *
 * AppTheme(componentTheme = customTheme) {
 *     // Your app content
 * }
 * ```
 */
data class ComponentTheme(
    val typography: TypographyTheme = TypographyTheme(),
    val button: ButtonTheme = ButtonTheme(),
    val alertDialog: AlertDialogTheme = AlertDialogTheme(),
    val surface: SurfaceTheme = SurfaceTheme(),
    val dialogHeader: DialogHeaderTheme = DialogHeaderTheme(),
    val iconButton: IconButtonTheme = IconButtonTheme(),
    val labeledSwitch: LabeledSwitchTheme = LabeledSwitchTheme(),
    val pickerField: PickerFieldTheme = PickerFieldTheme(),
    val customDropDownMenu: CustomDropDownMenuTheme = CustomDropDownMenuTheme(),
    val customTextField: CustomTextFieldTheme = CustomTextFieldTheme(),
    val datePicker: DatePickerTheme = DatePickerTheme(),
    val dateRangePicker: DateRangePickerTheme = DateRangePickerTheme(),
    val wheelDatePicker: WheelDatePickerTheme = WheelDatePickerTheme(),
    val header: HeaderTheme = HeaderTheme(),
    val searchField: SearchFieldTheme = SearchFieldTheme(),
    val progressIndicatorBars: ProgressIndicatorBarsTheme = ProgressIndicatorBarsTheme(),
    val optionItem: OptionItemTheme = OptionItemTheme(),
    val optionItemList: OptionItemListTheme = OptionItemListTheme(),
    val scrollbarWithScrollState: ScrollbarWithScrollStateTheme = ScrollbarWithScrollStateTheme(),
    val scrollbarWithLazyListState: ScrollbarWithLazyListStateTheme = ScrollbarWithLazyListStateTheme(),
    val jsonViewer: JsonViewerTheme = JsonViewerTheme(),
    val labeledSlot: LabeledSlotTheme = LabeledSlotTheme(),
    val navigationMenu: NavigationMenuTheme = NavigationMenuTheme(),
    val chipItem: ChipItemTheme = ChipItemTheme(),
    val chipsMenu: ChipsMenuTheme = ChipsMenuTheme(),
    val ratingRow: RatingRowTheme = RatingRowTheme(),
    val customSlider: CustomSliderTheme = CustomSliderTheme(),
    val otpView: OtpViewTheme = OtpViewTheme(),
    val swipeCards: SwipeCardsTheme = SwipeCardsTheme(),
    val reviewStack: ReviewStackTheme = ReviewStackTheme(),
    val tabRow: TabRowTheme = TabRowTheme(),
    val shutter: ShutterTheme = ShutterTheme(),
    val qrCodeView: QrCodeViewTheme = QrCodeViewTheme()
)

/**
 * Default component theme using library's default colors.
 * This is used when no custom ComponentTheme is provided.
 */
val DefaultComponentTheme = ComponentTheme()

/**
 * CompositionLocal for ComponentTheme.
 *
 * Library users can override component colors and typography by:
 * 1. Creating a custom ComponentTheme
 * 2. Passing it to AppTheme: `AppTheme(componentTheme = myCustomTheme) { ... }`
 *
 * Components will automatically use the theme values unless explicitly overridden
 * via component parameters.
 */
val LocalComponentTheme = compositionLocalOf { DefaultComponentTheme }

