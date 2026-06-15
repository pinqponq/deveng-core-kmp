package global.deveng.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import core.domain.camera.compose.DefaultCameraPreview
import core.domain.camera.compose.rememberCameraKState
import core.domain.camera.enums.AspectRatio
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.ImageFormat
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.permissions.Permissions
import core.domain.camera.permissions.providePermissions
import core.domain.camera.result.ImageCaptureResult
import core.domain.camera.scanner.QrScannerConfig
import core.domain.camera.scanner.QrScannerView
import core.domain.camera.state.CameraConfiguration
import core.presentation.permission.BindEffect
import core.presentation.permission.DeniedAlwaysException
import core.presentation.permission.DeniedException
import core.presentation.permission.Permission
import core.presentation.permission.PermissionState
import core.presentation.permission.rememberPermissionsControllerFactory
import core.domain.camera.state.CameraKEvent
import core.domain.camera.state.CameraKState
import core.domain.camera.state.CameraKStateHolder
import core.domain.camera.video.VideoCaptureResult
import core.presentation.component.CustomButton
import core.presentation.component.CustomDropDownMenu
import core.presentation.component.CustomHeader
import core.presentation.component.CustomIconButton
import core.presentation.component.CustomSlider
import core.presentation.component.LabeledSwitch
import core.presentation.component.PickerField
import core.presentation.component.RoundedSurface
import core.presentation.component.SearchField
import core.presentation.component.Shutter
import core.presentation.component.alertdialog.CustomAlertDialog
import core.presentation.component.chips.ChipItem
import core.presentation.component.chips.ChipsMenu
import core.presentation.component.datepicker.CustomDatePicker
import core.presentation.component.datepicker.CustomDateRangePicker
import core.presentation.component.datepicker.WheelDatePicker
import core.presentation.component.json.JsonViewer
import core.presentation.component.labeledslot.Label
import core.presentation.component.labeledslot.LabeledSlot
import core.presentation.component.lazyswipecards.SwipeCards
import core.presentation.component.lazyswipecards.rememberSwipeCardsState
import core.presentation.component.reviewstack.ReviewStack
import core.presentation.component.reviewstack.rememberReviewStackState
import core.util.markdown.MarkdownContentParser
import core.presentation.component.navigationmenu.MenuMode
import core.presentation.component.navigationmenu.NavigationMenu
import core.presentation.component.navigationmenu.NavigationMenuItem
import core.presentation.component.optionitemlist.OptionItem
import core.presentation.component.optionitemlist.OptionItemLazyListDialog
import core.presentation.component.qrcode.CustomQrCodeView
import core.presentation.component.optionitemlist.OptionItemList
import core.presentation.component.optionitemlist.OptionItemMultiSelectLazyListDialog
import core.presentation.component.otpview.OtpSize
import core.presentation.component.otpview.OtpView
import core.presentation.component.progressindicatorbars.IndicatorType
import core.presentation.component.progressindicatorbars.ProgressIndicatorBars
import core.presentation.component.ratingrow.RatingRow
import core.presentation.component.scrollbar.scrollbarWithLazyListState
import core.presentation.component.tabrow.LayoutStyle
import core.presentation.component.tabrow.TabRow
import core.presentation.component.textfield.CustomTextField
import core.presentation.component.textfield.DateTimeVisualTransformation
import core.presentation.theme.AlertDialogTheme
import core.presentation.theme.AppTheme
import core.presentation.theme.ButtonTheme
import core.presentation.theme.ComponentTheme
import core.presentation.theme.CoreBoldTextStyle
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.presentation.theme.CoreSemiBoldTextStyle
import core.presentation.theme.CustomTextFieldTheme
import core.presentation.theme.DatePickerTheme
import core.presentation.theme.DateRangePickerTheme
import core.presentation.theme.HeaderTheme
import core.presentation.theme.IconButtonTheme
import core.presentation.theme.JsonViewerTheme
import core.presentation.theme.LabeledSwitchTheme
import core.presentation.theme.NavigationMenuTheme
import core.presentation.theme.OptionItemListTheme
import core.presentation.theme.OptionItemTheme
import core.presentation.theme.ProgressIndicatorBarsTheme
import core.presentation.theme.ScrollbarWithLazyListStateTheme
import core.presentation.theme.ScrollbarWithScrollStateTheme
import core.presentation.theme.SearchFieldTheme
import core.presentation.theme.SurfaceTheme
import core.presentation.theme.SwipeCardsTheme
import core.presentation.theme.TypographyTheme
import core.presentation.theme.WheelDatePickerTheme
import core.util.datetime.CustomSelectableDates
import core.util.multiplatform.Platform
import core.util.datetime.TargetDates
import core.util.datetime.formatDateRange
import core.util.datetime.slashDateFormat
import core.util.image.PhotoSaveUtils
import core.util.image.SavePhotoResult
import core.util.video.SaveVideoResult
import core.util.video.VideoSaveUtils
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.deveng_logo
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_right
import deveng_core_kmp.sample.composeapp.generated.resources.ic_cyclone
import deveng_core_kmp.sample.composeapp.generated.resources.ic_dark_mode
import deveng_core_kmp.sample.composeapp.generated.resources.ic_rotate_right
import deveng_core_kmp.sample.composeapp.generated.resources.ic_undo
import deveng_core_kmp.sample.composeapp.generated.resources.theme
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
internal fun App() {
    val customTheme = ComponentTheme(
        // Custom font family (using system font as example)
        typography = TypographyTheme(
            fontFamily = FontFamily.SansSerif
        ),
        // Custom button theme
        button = ButtonTheme(
            containerColor = Color(0xFF1976D2),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF1976D2).copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.4f),
            defaultTextStyle = CoreSemiBoldTextStyle().copy(
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        ),
        // Custom alert dialog theme
        alertDialog = AlertDialogTheme(
            headerColor = Color(0xFFF5F5F5),
            bodyColor = Color.White,
            titleColor = Color(0xFF1A1C1C),
            descriptionColor = Color(0xFF404941),
            dividerColor = Color(0xFFD9D9D9),
            positiveButtonColor = Color(0xFF1976D2),
            positiveButtonTextColor = Color.White,
            negativeButtonColor = Color.White,
            negativeButtonTextColor = Color(0xFF1A1C1C),
            iconColor = Color(0xFF374151),
            titleTextStyle = CoreBoldTextStyle().copy(fontSize = 20.sp),
            descriptionTextStyle = CoreRegularTextStyle().copy(fontSize = 16.sp),
            buttonTextStyle = CoreMediumTextStyle().copy(fontSize = 16.sp)
        ),
        // Custom surface theme
        surface = SurfaceTheme(
            defaultColor = Color(0xFFF9F9F9),
            defaultContentColor = Color(0xFF1A1C1C)
        ),
        iconButton = IconButtonTheme(
            buttonSize = 48.dp,
            backgroundColor = Color(0xFFFDECEA),
            iconTint = Color(0xFFD32F2F),
            shadowElevation = 4.dp
        ),
        swipeCards = SwipeCardsTheme(
            buttonBackgroundColor = Color.White,
            buttonIconTint = Color(0xFF1A1C1C),
            buttonSize = 40.dp,
            buttonShadowElevation = 2.dp
        ),
        labeledSwitch = LabeledSwitchTheme(
            labelTextStyle = CoreMediumTextStyle().copy(
                fontSize = 15.sp,
                color = Color(0xFF0F172A)
            ),
            checkedTrackColor = Color(0xFF1976D2),
            uncheckedTrackColor = Color(0xFFE0E7FF),
            uncheckedBorderColor = Color(0xFF1976D2),
            switchScale = 0.85f
        ),
        optionItem = OptionItemTheme(
            backgroundColor = Color.White,
            textStyle = CoreMediumTextStyle().copy(
                fontSize = 16.sp,
                color = Color(0xFF0F172A)
            ),
            boldLeadingTextStyle = CoreBoldTextStyle().copy(
                fontSize = 16.sp,
                color = Color(0xFFD32F2F),
                textDecoration = TextDecoration.Underline
            ),
            checkIconTint = Color(0xFFD32F2F)
        ),
        optionItemList = OptionItemListTheme(
            containerColor = Color(0xFFFFF3E0),
            dividerColor = Color(0xFFFFCC80),
            containerShape = RoundedCornerShape(24.dp),
            lazyListScrollbarColor = Color.Red,
            lazyListScrollbarWidth = 6.dp
        ),
        customTextField = CustomTextFieldTheme(
            containerShape = RoundedCornerShape(20.dp),
            borderStroke = BorderStroke(1.dp, Color(0xFFE0E7FF)),
            containerColor = Color.White,
            textStyle = CoreMediumTextStyle().copy(
                fontSize = 16.sp,
                color = Color(0xFF0F172A)
            ),
            hintTextStyle = CoreMediumTextStyle().copy(
                fontSize = 16.sp,
                color = Color(0xFF94A3B8)
            ),
            cursorColor = Color(0xFF1976D2)
        ),
        datePicker = DatePickerTheme(
            trailingIconTint = Color(0xFF1976D2),
            dialogContainerColor = Color(0xFF0F172A),
            dialogContentColor = Color.White,
            selectedDayContainerColor = Color.White,
            selectedDayContentColor = Color(0xFF0F172A),
            selectedYearContainerColor = Color.White,
            selectedYearContentColor = Color(0xFF0F172A)
        ),
        dateRangePicker = DateRangePickerTheme(
            trailingIconTint = Color(0xFF1976D2),
            dialogContainerColor = Color.White,
            titleContentColor = Color(0xFF1976D2),
            selectedDayContainerColor = Color(0xFF1976D2),
            selectedDayContentColor = Color.White,
            todayContentColor = Color(0xFF1976D2),
            todayDateBorderColor = Color(0xFF1976D2),
            selectedYearContainerColor = Color(0xFF1976D2),
            selectedYearContentColor = Color.White,
            confirmButtonTextColor = Color(0xFF1976D2),
            dismissButtonTextColor = Color(0xFF1976D2)
        ),
        wheelDatePicker = WheelDatePickerTheme(
            trailingIconTint = Color(0xFF7C3AED),
            dialogContainerColor = Color(0xFF0B1020),
            dialogContentColor = Color(0xFFFFF59D),
            selectedItemTextStyle = CoreBoldTextStyle().copy(
                fontSize = 20.sp,
                color = Color(0xFFFF6B6B)
            ),
            unselectedItemTextStyle = CoreRegularTextStyle().copy(
                fontSize = 14.sp,
                color = Color(0xFF7DD3FC)
            ),
            labelTextStyle = CoreSemiBoldTextStyle().copy(
                fontSize = 12.sp,
                color = Color(0xFF86EFAC)
            ),
            selectorLineColor = Color(0xFFFFC107),
            confirmButtonTextColor = Color(0xFF22D3EE),
            dismissButtonTextColor = Color(0xFFF472B6)
        ),
        scrollbarWithScrollState = ScrollbarWithScrollStateTheme(
            scrollBarColor = Color.Red,
            scrollBarWidth = 15.dp,
            alwaysShowScrollBar = true
        ),
        scrollbarWithLazyListState = ScrollbarWithLazyListStateTheme(
            scrollBarColor = Color.Red,
            scrollBarWidth = 15.dp,
            alwaysShowScrollBar = true
        ),
        header = HeaderTheme(
            containerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            backgroundColor = Color(0xFFF5F5F5),
            leftIconTint = Color(0xFF1976D2),
            rightIconTint = Color(0xFF1976D2),
            leftIconBackgroundColor = Color(0xFFE3F2FD),
            rightIconBackgroundColor = Color(0xFFE3F2FD),
            iconButtonSize = 48.dp
        ),
        searchField = SearchFieldTheme(
            buttonSize = 42.dp,
            buttonShape = RoundedCornerShape(16.dp),
            buttonBackgroundColor = Color(0xFF1976D2),
            buttonIconTint = Color.White,
            buttonShadowElevation = 0.dp,
            defaultButtonIconDescription = "Search"
        ),
        progressIndicatorBars = ProgressIndicatorBarsTheme(
            filledIndicatorColor = Color(0xFF1976D2),
            defaultIndicatorColor = Color(0xFFE0E7FF),
            indicatorHeight = 8.dp,
            indicatorSpacing = 3.dp,
            indicatorCornerRadius = 3.dp
        ),
        jsonViewer = JsonViewerTheme(
            containerColor = Color(0xFFF9F9F9)
        ),
        navigationMenu = NavigationMenuTheme(
            backgroundColor = Color(0xFF111827),
            sectionSeparatorColor = Color(0xFF1F2937),
            horizontalItemSelectedBackgroundColor = Color(0xFF111827),
            verticalItemSelectedBackgroundColor = Color(0xFF111827),
            itemUnselectedBackgroundColor = Color(0xFF111827),
            verticalDividerColor = Color(0xFF374151)
        )
    )

    // Step 2: Apply theme using library's AppTheme
    AppTheme(componentTheme = customTheme) {
        var showCameraScreen by remember { mutableStateOf(false) }
        var showQrGeneratorScreen by remember { mutableStateOf(false) }
        var showQrScannerScreen by remember { mutableStateOf(false) }
        when {
            showQrGeneratorScreen -> QrGeneratorDemoScreen(onBack = { showQrGeneratorScreen = false })
            showQrScannerScreen -> QrScannerDemoScreen(onBack = { showQrScannerScreen = false })
            showCameraScreen -> CameraScreen(onBack = { showCameraScreen = false })
            else -> ThemingDemo(
                onOpenCamera = { showCameraScreen = true },
                onOpenQrGenerator = { showQrGeneratorScreen = true },
                onOpenQrScanner = { showQrScannerScreen = true },
            )
        }
    }
}

@Composable
private fun CameraScreen(onBack: () -> Unit) {
    val permissions: Permissions = providePermissions()
    var hasCameraPermission by remember { mutableStateOf(permissions.hasCameraPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Request camera permission first (per CameraK README / Sample)
    if (!hasCameraPermission && !permissionDenied) {
        permissions.RequestCameraPermission(
            onGranted = { hasCameraPermission = true },
            onDenied = { permissionDenied = true },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when {
            permissionDenied -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Camera permission is required to use the camera.",
                        color = Color.White,
                        style = CoreRegularTextStyle().copy(fontSize = 16.sp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            }

            !hasCameraPermission -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = "Requesting camera permission...",
                            color = Color.White,
                            style = CoreRegularTextStyle().copy(fontSize = 16.sp),
                        )
                    }
                }
            }

            else -> CameraContent(onBack = onBack)
        }
    }
}

@Composable
private fun CameraContent(onBack: () -> Unit) {
    var aspectRatio by remember { mutableStateOf(AspectRatio.RATIO_9_16) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var imageFormat by remember { mutableStateOf(ImageFormat.JPEG) }
    var qualityPrioritization by remember { mutableStateOf(QualityPrioritization.BALANCED) }

    val config = remember(aspectRatio, flashMode, imageFormat, qualityPrioritization) {
        CameraConfiguration(
            cameraLens = CameraLens.BACK,
            flashMode = flashMode,
            aspectRatio = aspectRatio,
            imageFormat = imageFormat,
            qualityPrioritization = qualityPrioritization,
        )
    }

    var cameraStateHolder by remember { mutableStateOf<CameraKStateHolder?>(null) }
    val cameraState by rememberCameraKState(
        config = config,
        setupPlugins = {},
        onHolder = { cameraStateHolder = it },
    )

    LaunchedEffect(cameraStateHolder) {
        val holder = cameraStateHolder ?: return@LaunchedEffect
        holder.events
            .filterIsInstance<CameraKEvent.RecordingStopped>()
            .collect { event ->
                when (val r = event.result) {
                    is VideoCaptureResult.Success -> {
                        when (val saveResult = VideoSaveUtils.saveVideoToPhotos(r.filePath)) {
                            is SaveVideoResult.Success ->
                                println("Video saved to gallery: ${saveResult.path} (${r.durationMs}ms)")

                            is SaveVideoResult.Error ->
                                println("Video save to gallery failed: ${saveResult.exception.message}")
                        }
                    }

                    is VideoCaptureResult.Error ->
                        println("Video error: ${r.exception.message}")
                }
            }
    }

    @Composable
    fun ThumbnailCountBadge(count: Int) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Black, CircleShape)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.coerceAtLeast(1).toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    @Composable
    fun FakeBottomNavigation() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Home", "Search", "Create", "Inbox", "Profile").forEachIndexed { index, label ->
                val active = index == 2
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(width = 56.dp, height = 36.dp),
                ) {
                    Text(
                        text = label,
                        color = if (active) Color.White else Color.White.copy(alpha = 0.55f),
                        style = CoreRegularTextStyle().copy(
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = cameraState) {
            is CameraKState.Initializing -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = "Initializing camera...",
                            color = Color.White,
                            style = CoreRegularTextStyle().copy(fontSize = 16.sp),
                        )
                    }
                }
            }

            is CameraKState.Ready -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    DefaultCameraPreview(
                        controller = state.controller,
                        hostPlatform = Platform.ANDROID,
                        maxVideoRecordingDurationMs = 90_000L,
                        modifier = Modifier.padding(bottom = 66.dp),
                        thumbnailTopEndContent = {
                            ThumbnailCountBadge(count = 22)
                        },
                        onImageCaptured = { result ->
                            when (result) {
                                is ImageCaptureResult.Success -> {
                                    val path = getNewPhotoSavePath()
                                    val dummyLat = 41.0082
                                    val dummyLon = 28.9784
                                    val bytesToSave = PhotoSaveUtils.addLocationExif(
                                        result.byteArray,
                                        dummyLat,
                                        dummyLon
                                    )
                                    when (val saveResult =
                                        PhotoSaveUtils.savePhoto(bytesToSave, path)) {
                                        is SavePhotoResult.Success -> { /* saved */
                                        }

                                        is SavePhotoResult.Error -> { /* handle error */
                                        }
                                    }
                                }

                                is ImageCaptureResult.Error ->
                                    println("Error: ${result.exception.message}")
                            }
                        },
                        onGalleryClick = { /* TODO: open gallery */ },
                        onLastPhotoClick = { bitmap -> /* TODO: open viewer with bitmap */ },
                        stateHolder = cameraStateHolder,
                    )

                    // Visual-only mock bottom navigation for layout preview.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    ) {
                        FakeBottomNavigation()
                    }
                }
            }

            is CameraKState.Error -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                        style = CoreRegularTextStyle().copy(fontSize = 16.sp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemingDemo(
    onOpenCamera: () -> Unit = {},
    onOpenQrGenerator: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    var showDefaultDialog by remember { mutableStateOf(false) }
    var showMarkdownDialog by remember { mutableStateOf(false) }
    var showOptionDialog by remember { mutableStateOf(false) }
    var showMultiSelectDialog by remember { mutableStateOf(false) }
    val sampleOptions = remember {
        listOf(
            "Themed option A",
            "Themed option B",
            "Themed option C",
            "Themed option D"
        )
    }
    val clickOption by remember { mutableStateOf(true) }
    var selectedOption by remember { mutableStateOf(sampleOptions.first()) }
    var selectedOptions by remember { mutableStateOf(setOf<String>()) }
    var labeledSwitchEnabled by remember { mutableStateOf(true) }
    var labeledSwitchCustom by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var pickerSelection by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var wheelSelectedDate by remember { mutableStateOf<LocalDate?>(LocalDate(2026, 4, 28)) }
    var selectedStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var searchText by remember { mutableStateOf("") }
    var amountValue by remember { mutableStateOf("125") }
    var borderOverrideValue by remember { mutableStateOf("") }
    var dateTimeValue by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    val selectableDates = remember { CustomSelectableDates() }
    val selectableDatesPast = remember { CustomSelectableDates() }
    val selectableDatesFuture = remember { CustomSelectableDates() }
    val priorityOptions = remember {
        listOf("Critical", "High", "Medium", "Low")
    }
    val statusOptions = remember {
        listOf(
            "Open",
            "In Progress",
            "Resolved",
            "Resolved",
            "Resolved",
            "Resolved",
            "Resolved",
            "Resolved",
            "Resolved",
            "Resolved",
            "Resolved"
        )
    }
    var selectedPriority by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    val lazyListState = rememberLazyListState()
    var messageCount by remember { mutableStateOf<Int?>(5) }
    var isChipSelected by remember { mutableStateOf(false) }
    var isChipCountSectionVisible by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableStateOf(0) }
    var otpDigits by remember { mutableStateOf("") }
    var otpDigits4 by remember { mutableStateOf("") }
    var otpDigitsError by remember { mutableStateOf("") }
    var otpDigitsAlphanumeric by remember { mutableStateOf("") }
    var isOtpError by remember { mutableStateOf(false) }
    val sampleMarkdown = remember {
        """
        # Terms
        ## Account
        - You accept **safe usage** rules.
        - You can cancel _anytime_.
        ### Notes
        Keep this text short for demo.
        """.trimIndent()
    }

    val swipeCardsDemoImages = remember {
        List(12) { Res.drawable.ic_cyclone }
    }
    val swipeCardsDemoState = rememberSwipeCardsState()

    val navigationMenuItems = listOf(
        NavigationMenuItem(
            screenRoute = "dashboard",
            icon = Res.drawable.ic_rotate_right,
            iconTint = Color.White,
            iconDescription = Res.string.theme,
            text = Res.string.theme,
            textColor = Color.White
        ),
        NavigationMenuItem(
            screenRoute = "logs",
            icon = Res.drawable.ic_dark_mode,
            iconTint = Color.White,
            iconDescription = Res.string.theme,
            text = Res.string.theme,
            textColor = Color.White
        ),
        NavigationMenuItem(
            screenRoute = "dashboard",
            icon = Res.drawable.ic_rotate_right,
            iconTint = Color.White,
            iconDescription = Res.string.theme,
            text = Res.string.theme,
            textColor = Color.White
        ),
        NavigationMenuItem(
            screenRoute = "logs",
            icon = Res.drawable.ic_dark_mode,
            iconTint = Color.White,
            iconDescription = Res.string.theme,
            text = Res.string.theme,
            textColor = Color.White
        )
    )

    Column {
        NavigationMenu(
            isExpanded = isExpanded,
            menuMode = MenuMode.Horizontal,
            itemList = navigationMenuItems,
            expandedLeadingSlot = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Navigation",
                        style = CoreSemiBoldTextStyle().copy(
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Themed side menu using ComponentTheme",
                        style = CoreRegularTextStyle().copy(
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
            },
            expandedTrailingSlot = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "v1.0.0",
                        style = CoreRegularTextStyle().copy(
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
            },
            collapsedLeadingSlot = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "N",
                        style = CoreSemiBoldTextStyle().copy(
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    )
                }
            },
            collapsedTrailingSlot = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "v1",
                        style = CoreRegularTextStyle().copy(
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
            },
            onItemClick = { },
            isItemSelected = { true },
            itemText = { stringResource(it.text) },
            itemTextStyle = { CoreMediumTextStyle().copy() },
            itemIcon = { it.icon },
            itemIconTint = { it.iconTint },
            itemIconDescription = { it.iconDescription?.let { stringResource(it) } }
        )

        Row {
            NavigationMenu(
                isExpanded = isExpanded,
                menuMode = MenuMode.Vertical,
                itemList = navigationMenuItems,
                expandedLeadingSlot = {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Navigation",
                            style = CoreSemiBoldTextStyle().copy(
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Themed side menu using ComponentTheme",
                            style = CoreRegularTextStyle().copy(
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                },
                expandedTrailingSlot = {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "v1.0.0",
                            style = CoreRegularTextStyle().copy(
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                },
                collapsedLeadingSlot = {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "N",
                            style = CoreSemiBoldTextStyle().copy(
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        )
                    }
                },
                collapsedTrailingSlot = {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "v1",
                            style = CoreRegularTextStyle().copy(
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                },
                onItemClick = { },
                isItemSelected = { true },
                itemText = { stringResource(it.text) },
                itemTextStyle = { CoreMediumTextStyle().copy() },
                itemIcon = { it.icon },
                itemIconTint = { it.iconTint },
                itemIconDescription = { it.iconDescription?.let { stringResource(it) } }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .scrollbarWithLazyListState(lazyListState),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionTitle("SwipeCards Examples")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 16f)
                        ) {
                            SwipeCards(
                                modifier = Modifier.fillMaxSize(),
                                state = swipeCardsDemoState,
                                visibleItemCount = 4,
                                showSwipeButtons = true,
                                negativeButtonIcon = Res.drawable.ic_arrow_left,
                                positiveButtonIcon = Res.drawable.ic_arrow_right,
                                revertButtonIcon = Res.drawable.ic_undo,
                                onSwipeLeft = { key -> println("SwipeCards: swiped left key=$key") },
                                onSwipeRight = { key -> println("SwipeCards: swiped right key=$key") },
                                onRevert = { key -> println("SwipeCards: reverted key=$key") },
                                content = {
                                    itemsIndexed(swipeCardsDemoImages, key = { idx, _ -> idx }) { _, image ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF101418)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(image),
                                                contentDescription = null,
                                                modifier = Modifier.size(180.dp),
                                            )
                                        }
                                    }
                                    onSwiped { _, _ -> }
                                }
                            )
                        }

                        SectionTitle("ReviewStack Examples")

                        val reviewStackState = rememberReviewStackState()
                        ReviewStack(
                            modifier = Modifier.fillMaxWidth(),
                            items = swipeCardsDemoImages,
                            key = { it.toString() },
                            state = reviewStackState,
                            previousIcon = Res.drawable.ic_arrow_left,
                            nextIcon = Res.drawable.ic_arrow_right,
                            negativeIcon = Res.drawable.ic_arrow_left,
                            positiveIcon = Res.drawable.ic_arrow_right,
                            onDecision = { _, decision ->
                                println("ReviewStack: decision=$decision")
                            },
                            itemContent = { image ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(image),
                                        contentDescription = null,
                                        modifier = Modifier.size(180.dp),
                                    )
                                }
                            },
                        )

                        Text(
                            text = "ComponentTheme Demo",
                            style = CoreBoldTextStyle().copy(fontSize = 24.sp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        SectionTitle("RatingRowComponent Examples")

                        CustomButton(
                            text = "Open Camera",
                            containerColor = Color(0xFF4CAF50),
                            onClick = onOpenCamera
                        )

                        CustomButton(
                            text = "Open QR Generator",
                            containerColor = Color(0xFF9C27B0),
                            onClick = onOpenQrGenerator
                        )

                        CustomButton(
                            text = "Open QR Scanner",
                            containerColor = Color(0xFF2196F3),
                            onClick = onOpenQrScanner
                        )

                        RatingRow(
                            maxRating = 5,
                            iconSize = 45.dp,
                            onRatingChanged = {}
                        )

                        SectionTitle("Shutter Examples")

                        var isShutterExpanded by remember { mutableStateOf(false) }

                        Shutter(
                            title = "Title",
                            description = "Description",
                            isShutterExpanded = isShutterExpanded,
                            onClickShutter = { isShutterExpanded = !isShutterExpanded },
                            content = {
                                Column(
                                    modifier = Modifier
                                        .height(200.dp)
                                        .fillMaxWidth()
                                        .background(color = Color.Gray),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    var volume by remember { mutableStateOf(45f) }

                                    CustomSlider(
                                        title = "Volume",
                                        value = volume,
                                        minValue = 0f,
                                        maxValue = 100f,
                                        steps = 0,
                                        isEnabled = true,
                                        isRangeLabelsVisible = false,
                                        valueFormatter = { "${it.toInt()} %" },
                                        leadingSlot = {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_arrow_left),
                                                contentDescription = "Close Volume",
                                                tint = Color.Green
                                            )
                                        },
                                        trailingSlot = {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_arrow_right),
                                                contentDescription = "Open Volume",
                                                tint = Color.Green
                                            )
                                        },
                                        onValueChange = { volume = it },
                                        onValueChangeFinished = {}
                                    )

                                    var loanAmount by remember { mutableStateOf(15000f) }
                                    val isError = loanAmount > 40000f

                                    CustomSlider(
                                        title = "Total Amount",
                                        value = loanAmount,
                                        minValue = 5000f,
                                        maxValue = 50000f,
                                        steps = 8,
                                        isEnabled = true,
                                        isRangeLabelsVisible = true,
                                        errorMessage = if (isError) "This amount is too high." else null,
                                        sliderColors = if (isError) SliderDefaults.colors(
                                            thumbColor = Color.Red,
                                            activeTrackColor = Color.Red
                                        )
                                        else SliderDefaults.colors(),
                                        onValueChange = { loanAmount = it },
                                        onValueChangeFinished = {}
                                    )
                                }
                            }
                        )

                        SectionTitle("TabRow Examples")

                        var selectedScrollableTab by remember { mutableStateOf("Tab 1") }
                        var selectedFixedTab by remember { mutableStateOf("Tab 1") }

                        TabRow(
                            tabs = listOf(
                                "Tab 1",
                                "Tab 2",
                                "Tab 3",
                                "Tab 4",
                                "Tab 5",
                                "Tab 6",
                                "Tab 7",
                                "Tab 7789"
                            ),
                            selectedTab = selectedScrollableTab,
                            tabTitle = { it },
                            onClickTabItem = { selectedScrollableTab = it },
                            layoutStyle = LayoutStyle.SCROLLABLE,
                            content = {
                                Text(text = it)
                            }
                        )

                        TabRow(
                            tabs = listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5", "Tab 6"),
                            selectedTab = selectedFixedTab,
                            tabTitle = { it },
                            onClickTabItem = { selectedFixedTab = it },
                            layoutStyle = LayoutStyle.FIXED,
                            content = {
                                Text(text = it)
                            }
                        )

                        SectionTitle("ChipsMenu Examples")

                        var selectedChipItem by remember { mutableStateOf<String?>(null) }

                        ChipsMenu(
                            optionList = listOf(
                                "Item 1",
                                "Item 2",
                                "Item 3",
                                "Item 4",
                                "Item 5",
                                "Item 6"
                            ),
                            selectedOption = selectedChipItem,
                            optionText = { it },
                            onOptionClick = { clickedItem ->
                                selectedChipItem = clickedItem
                            }
                        )

                        SectionTitle("CustomButton Examples")

                        CustomButton(
                            text = "Side Menu Opener and Closer",
                            onClick = { isExpanded = !isExpanded }
                        )

                        CustomButton(
                            text = "Themed Button",
                            onClick = { }
                        )

                        CustomButton(
                            text = "Overridden Color",
                            containerColor = Color(0xFFE91E63),
                            onClick = { }
                        )

                        CustomButton(
                            text = "Custom Typography",
                            textStyle = CoreBoldTextStyle().copy(fontSize = 20.sp),
                            onClick = { }
                        )

                        CustomButton(
                            text = "Disabled Button",
                            enabled = false,
                            onClick = { }
                        )

                        SectionTitle("RoundedSurface Examples")

                        RoundedSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Themed Surface",
                                    style = CoreMediumTextStyle().copy(fontSize = 16.sp)
                                )
                            }
                        }

                        RoundedSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Custom Color Surface",
                                    style = CoreSemiBoldTextStyle().copy(fontSize = 16.sp)
                                )
                            }
                        }

                        SectionTitle("LabeledSlot Examples")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LabeledSlot(
                                labels = listOf(
                                    Label(
                                        text = "NEW",
                                        alignment = Alignment.TopEnd,
                                        backgroundColor = Color(0xFFD32F2F),
                                        textColor = Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                ),
                                contentSlot = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Image",
                                            style = CoreMediumTextStyle().copy(fontSize = 14.sp)
                                        )
                                    }
                                }
                            )

                            LabeledSlot(
                                labels = listOf(
                                    Label(
                                        text = "SALE",
                                        alignment = Alignment.TopStart,
                                        backgroundColor = Color(0xFF1976D2),
                                        textColor = Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                ),
                                contentSlot = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Image",
                                            style = CoreMediumTextStyle().copy(fontSize = 14.sp)
                                        )
                                    }
                                }
                            )
                        }

                        LabeledSlot(
                            labels = listOf(
                                Label(
                                    text = "TOP",
                                    alignment = Alignment.TopStart,
                                    backgroundColor = Color(0xFF4CAF50),
                                    textColor = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                                Label(
                                    text = "HOT",
                                    alignment = Alignment.TopEnd,
                                    backgroundColor = Color(0xFFE91E63),
                                    textColor = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ),
                            contentSlot = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Multiple Labels",
                                        style = CoreMediumTextStyle().copy(fontSize = 14.sp)
                                    )
                                }
                            }
                        )

                        LabeledSlot(
                            modifier = Modifier.fillMaxWidth(),
                            labels = listOf(
                                Label(
                                    text = "CUSTOM",
                                    alignment = Alignment.BottomCenter,
                                    backgroundColor = Color(0xFF1976D2),
                                    textColor = Color.White,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            ),
                            containerWidth = null,
                            containerHeight = 120.dp,
                            containerShape = RoundedCornerShape(20.dp),
                            contentSlot = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFE3F2FD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Custom Size & Shape",
                                        style = CoreSemiBoldTextStyle().copy(fontSize = 16.sp)
                                    )
                                }
                            }
                        )

                        SectionTitle("Typography Examples")

                        Text(
                            text = "Regular Text (400)",
                            style = CoreRegularTextStyle().copy(fontSize = 16.sp)
                        )

                        Text(
                            text = "Medium Text (500)",
                            style = CoreMediumTextStyle().copy(fontSize = 16.sp)
                        )

                        Text(
                            text = "SemiBold Text (600)",
                            style = CoreSemiBoldTextStyle().copy(fontSize = 16.sp)
                        )

                        Text(
                            text = "Bold Text (700)",
                            style = CoreBoldTextStyle().copy(fontSize = 16.sp)
                        )

                        Text(
                            text = "Custom Styled Text",
                            style = CoreSemiBoldTextStyle().copy(
                                fontSize = 18.sp,
                                color = Color(0xFF1976D2)
                            )
                        )

                        SectionTitle("CustomAlertDialog Examples")

                        CustomButton(
                            text = "Show Themed Dialog",
                            onClick = { showDialog = true }
                        )

                        CustomButton(
                            text = "Show Default Dialog",
                            containerColor = Color(0xFF4CAF50),
                            onClick = { showDefaultDialog = true }
                        )

                        CustomButton(
                            text = "Show Markdown Dialog",
                            containerColor = Color(0xFF7C4DFF),
                            onClick = { showMarkdownDialog = true }
                        )

                        CustomAlertDialog(
                            isDialogVisible = showDialog,
                            title = "Themed Dialog",
                            description = "This dialog uses colors and typography from ComponentTheme. All text uses the custom font family.",
                            positiveButtonText = "OK",
                            negativeButtonText = "Cancel",
                            onPositiveButtonClick = { showDialog = false },
                            onNegativeButtonClick = { showDialog = false },
                            onDismissRequest = { showDialog = false }
                        )

                        CustomAlertDialog(
                            isDialogVisible = showDefaultDialog,
                            title = "Default Dialog",
                            descriptionAnnotated = buildAnnotatedString {
                                withStyle(
                                    style = CoreBoldTextStyle().copy(
                                        fontSize = 16.sp
                                    ).toSpanStyle()
                                ) {
                                    append("Username")
                                }

                                withStyle(
                                    style = CoreMediumTextStyle().copy(
                                        fontSize = 14.sp,
                                        color = Color.Red
                                    ).toSpanStyle()
                                ) {
                                    append(" has been deleted.")
                                }
                            },
                            titleColor = Color(0xFF4CAF50),
                            descriptionColor = Color(0xFF666666),
                            positiveButtonText = "Confirm",
                            positiveButtonColor = Color(0xFF4CAF50),
                            positiveButtonTextColor = Color.White,
                            negativeButtonText = "Cancel",
                            onPositiveButtonClick = { showDefaultDialog = false },
                            onNegativeButtonClick = { showDefaultDialog = false },
                            onDismissRequest = { showDefaultDialog = false }
                        )

                        if (showMarkdownDialog) {
                            Dialog(onDismissRequest = { showMarkdownDialog = false }) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF1E1E1E))
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Markdown Parser Demo",
                                            style = CoreSemiBoldTextStyle().copy(
                                                fontSize = 18.sp,
                                                color = Color.White
                                            )
                                        )
                                        MarkdownContentParser(
                                            markdownContent = sampleMarkdown,
                                            textColor = Color.Red,
                                        )
                                        CustomButton(
                                            text = "Close",
                                            onClick = { showMarkdownDialog = false }
                                        )
                                    }
                                }
                            }
                        }

                        SectionTitle("Scrollbar Examples")

                        Text(
                            text = "Entire page now uses a LazyColumn with the red 15.dp scrollbar.",
                            style = CoreRegularTextStyle().copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        SectionTitle("OptionItemList Examples")

                        OptionItemList(
                            optionList = sampleOptions,
                            optionText = { it },
                            optionId = { it.hashCode() },
                            isCheckIconsVisible = true,
                            selectedOption = selectedOption,
                            onOptionItemClick = {
                                selectedOption = it
                            }
                        )

                        CustomButton(
                            text = "Show Option Dialog",
                            onClick = { showOptionDialog = true }
                        )

                        SectionTitle("LabeledSwitch Examples")

                        LabeledSwitch(
                            label = "Notifications",
                            isChecked = labeledSwitchEnabled,
                            onSwitchClick = { labeledSwitchEnabled = it }
                        )

                        LabeledSwitch(
                            label = "Custom Colored Switch",
                            isChecked = labeledSwitchCustom,
                            checkedTrackColor = Color(0xFFE91E63),
                            uncheckedThumbColor = Color(0xFFE91E63),
                            uncheckedTrackColor = Color.White,
                            switchScale = 1f,
                            onSwitchClick = { labeledSwitchCustom = it }
                        )

                        SectionTitle("CustomTextField Examples")

                        val shouldShowNameError =
                            textFieldValue.isNotEmpty() && textFieldValue.length < 3

                        CustomTextField(
                            title = "Name",
                            value = textFieldValue,
                            hint = "Enter at least 3 characters",
                            isTextCharCountVisible = true,
                            errorMessage = if (shouldShowNameError) "Minimum 3 characters" else null,
                            onValueChange = { textFieldValue = it }
                        )

                        CustomTextField(
                            title = "Password",
                            value = passwordValue,
                            hint = "Password",
                            keyboardType = KeyboardType.Password,
                            isPasswordVisible = isPasswordVisible,
                            onPasswordToggleClick = { isPasswordVisible = it },
                            onValueChange = { passwordValue = it }
                        )

                        CustomTextField(
                            title = "Inline Suffix Field",
                            value = amountValue,
                            hint = "Enter amount",
                            inlineSuffix = " kg",
                            suffixSlot = {
                                Text(
                                    text = "kg",
                                    style = CoreMediumTextStyle().copy(color = Color(0xFF0F172A))
                                )
                            },
                            titleTrailingSlot = {
                                Text(
                                    text = "Max 3 digits",
                                    style = CoreRegularTextStyle().copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                )
                            },
                            maxLength = 3,
                            onValueChange = { newValue ->
                                amountValue = newValue.filter { it.isDigit() }
                            }
                        )

                        CustomTextField(
                            title = "Border Overrides",
                            value = borderOverrideValue,
                            hint = "Custom focus colors",
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFF94A3B8),
                            focusedBorderWidth = 2.dp,
                            unfocusedBorderWidth = 1.dp,
                            onValueChange = { borderOverrideValue = it }
                        )

                        CustomTextField(
                            title = "Date Time",
                            value = dateTimeValue,
                            hint = "DD-MM-YYYY HH:MM",
                            maxLength = 12,
                            keyboardType = KeyboardType.Number,
                            visualTransformation = DateTimeVisualTransformation(),
                            onValueChange = { newValue ->
                                dateTimeValue = newValue.filter { it.isDigit() }
                            }
                        )

                        SectionTitle("CustomDropDownMenu Examples")

                        CustomDropDownMenu(
                            title = "Priority",
                            hintMessage = "Select priority",
                            items = priorityOptions,
                            selectedItem = selectedPriority,
                            onItemSelected = { selectedPriority = it },
                            isScrollBarVisible = false
                        )

                        CustomDropDownMenu(
                            title = "Status",
                            hintMessage = "Pick status",
                            backgroundColor = Color(0xFF111827),
                            textColor = Color(0xFFF8FAFC),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedBorderColor = Color(0xFF38BDF8),
                            dividerColor = Color(0xFF1E293B),
                            scrollBarColor = Color(0xFF38BDF8),
                            isScrollBarVisible = true,
                            items = statusOptions,
                            selectedItem = selectedStatus,
                            onItemSelected = { selectedStatus = it }
                        )

                        SectionTitle("JsonViewer Examples")

                        val sampleJson = remember {
                            """
                                [\n  {\n    \"id\": 7,\n    \"name\": \"Adana\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 8,\n    \"name\": \"Adıyaman\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 9,\n    \"name\": \"Afyonkarahisar\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 10,\n    \"name\": \"Ağrı\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 11,\n    \"name\": \"Aksaray\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 12,\n    \"name\": \"Amasya\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 13,\n    \"name\": \"Ankara\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 14,\n    \"name\": \"Antalya\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 15,\n    \"name\": \"Ardahan\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 16,\n    \"name\": \"Artvin\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 17,\n    \"name\": \"Aydın\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 18,\n    \"name\": \"Balıkesir\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 19,\n    \"name\": \"Bartın\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 20,\n    \"name\": \"Batman\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 21,\n    \"name\": \"Bayburt\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 22,\n    \"name\": \"Bilecik\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 23,\n    \"name\": \"Bingöl\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 24,\n    \"name\": \"Bitlis\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 25,\n    \"name\": \"Bolu\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 26,\n    \"name\": \"Burdur\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 27,\n    \"name\": \"Bursa\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 28,\n    \"name\": \"Çanakkale\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 29,\n    \"name\": \"Çankırı\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 30,\n    \"name\": \"Çorum\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 31,\n    \"name\": \"Denizli\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 32,\n    \"name\": \"Diyarbakır\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 33,\n    \"name\": \"Düzce\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 34,\n    \"name\": \"Edirne\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 35,\n    \"name\": \"Elazığ\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 36,\n    \"name\": \"Erzincan\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 37,\n    \"name\": \"Erzurum\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 38,\n    \"name\": \"Eskişehir\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 39,\n    \"name\": \"Gaziantep\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 40,\n    \"name\": \"Giresun\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 41,\n    \"name\": \"Gümüşhane\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 42,\n    \"name\": \"Hakkari\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 43,\n    \"name\": \"Hatay\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 44,\n    \"name\": \"Iğdır\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 45,\n    \"name\": \"Isparta\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 46,\n    \"name\": \"İstanbul\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 47,\n    \"name\": \"İzmir\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 48,\n    \"name\": \"Kahramanmaraş\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 49,\n    \"name\": \"Karabük\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 50,\n    \"name\": \"Karaman\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 51,\n    \"name\": \"Kars\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 52,\n    \"name\": \"Kastamonu\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 53,\n    \"name\": \"Kayseri\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 54,\n    \"name\": \"Kilis\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 55,\n    \"name\": \"Kırıkkale\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 56,\n    \"name\": \"Kırklareli\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 57,\n    \"name\": \"Kırşehir\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 58,\n    \"name\": \"Kocaeli\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 59,\n    \"name\": \"Konya\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 60,\n    \"name\": \"Kütahya\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 61,\n    \"name\": \"Malatya\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 62,\n    \"name\": \"Manisa\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 63,\n    \"name\": \"Mardin\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 64,\n    \"name\": \"Mersin\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 65,\n    \"name\": \"Muğla\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 66,\n    \"name\": \"Muş\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 67,\n    \"name\": \"Nevşehir\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 68,\n    \"name\": \"Niğde\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 69,\n    \"name\": \"Ordu\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 70,\n    \"name\": \"Osmaniye\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 71,\n    \"name\": \"Rize\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 72,\n    \"name\": \"Sakarya\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 73,\n    \"name\": \"Samsun\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 74,\n    \"name\": \"Şanlıurfa\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 75,\n    \"name\": \"Siirt\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 76,\n    \"name\": \"Sinop\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 77,\n    \"name\": \"Sivas\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 78,\n    \"name\": \"Şırnak\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 79,\n    \"name\": \"Tekirdağ\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 80,\n    \"name\": \"Tokat\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 81,\n    \"name\": \"Trabzon\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 82,\n    \"name\": \"Tunceli\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 83,\n    \"name\": \"Uşak\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 84,\n    \"name\": \"Van\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 85,\n    \"name\": \"Yalova\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 86,\n    \"name\": \"Yozgat\",\n    \"countryId\": 7,\n    \"isActive\": true\n  },\n  {\n    \"id\": 87,\n    \"name\": \"Zonguldak\",\n    \"countryId\": 7,\n    \"isActive\": true\n  }\n]                            
                            """
                        }

                        JsonViewer(
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState()),
                            title = "User Data",
                            json = sampleJson,
                            onClickCopyJsonIcon = {}
                        )

                        SectionTitle("SearchField Examples")

                        SearchField(
                            searchText = searchText,
                            onSearchTextChange = { searchText = it },
                            searchBarHint = "Search...",
                            buttonIcon = Res.drawable.ic_cyclone,
                            buttonIconDescription = "Search",
                            isButtonVisible = false,
                            trailingSlot = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_cyclone),
                                    contentDescription = "Search",
                                    tint = Color.Black
                                )
                            },
                            onTypingStop = {
                                println("You can search now")
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .padding(10.dp)
                        ) {
                            SearchField(
                                searchText = searchText,
                                onSearchTextChange = { searchText = it },
                                onSearchButtonClick = { },
                                searchBarHint = "Mesajlarda Ara..",
                                buttonIcon = Res.drawable.ic_cyclone,
                                buttonIconDescription = "Search",
                                buttonSize = 46.dp,
                                buttonShape = RoundedCornerShape(16.dp),
                                buttonBackgroundColor = Color(0xFF167D5C),
                                textFieldShape = RoundedCornerShape(16.dp),
                                textFieldModifier = Modifier.height(46.dp),
                                textStyle = CoreMediumTextStyle().copy(
                                    fontSize = 12.sp
                                ),
                                hintTextStyle = CoreMediumTextStyle().copy(
                                    fontSize = 12.sp,
                                    color = Color(0XFFB8B8B8)
                                )
                            )
                        }

                        SearchField(
                            searchText = searchText,
                            onSearchTextChange = { searchText = it },
                            onSearchButtonClick = { },
                            searchBarHint = "Custom styled search",
                            buttonIcon = Res.drawable.ic_cyclone,
                            buttonIconDescription = "Search",
                            buttonSize = 48.dp,
                            buttonShape = RoundedCornerShape(12.dp),
                            buttonBackgroundColor = Color(0xFF167D5C),
                            textFieldShape = RoundedCornerShape(16.dp),
                            textFieldModifier = Modifier.height(48.dp),
                            textStyle = CoreMediumTextStyle().copy(
                                fontSize = 14.sp
                            ),
                            hintTextStyle = CoreMediumTextStyle().copy(
                                fontSize = 14.sp,
                                color = Color(0XFFB8B8B8)
                            )
                        )

                        SearchField(
                            searchText = searchText,
                            onSearchTextChange = { searchText = it },
                            onSearchButtonClick = { },
                            searchBarHint = "Search with button at start",
                            isButtonAtEnd = false,
                            buttonIcon = Res.drawable.ic_cyclone,
                            buttonIconDescription = "Search",
                            buttonBackgroundColor = Color(0xFFE91E63),
                            buttonIconTint = Color.White
                        )

                        SectionTitle("Chip Item Examples")

                        ChipItem(
                            text = "Chip 1",
                            count = messageCount,
                            isSelected = isChipSelected,
                            isCountSectionVisible = isChipCountSectionVisible,
                            onClick = {
                                isChipSelected = !isChipSelected
                                isChipCountSectionVisible = false
                            }
                        )

                        ChipItem(
                            leadingIcon = Res.drawable.ic_cyclone,
                            text = "Chip 2",
                            count = messageCount,
                            isSelected = isChipSelected,
                            isCountSectionVisible = isChipCountSectionVisible,
                            onClick = {
                                isChipSelected = !isChipSelected
                                isChipCountSectionVisible = false
                            }
                        )

                        ChipItem(
                            leadingIcon = Res.drawable.ic_cyclone,
                            text = "Chip 3",
                            isCountSectionVisible = false,
                            isSelected = isChipSelected,
                            onClick = {
                                isChipSelected = !isChipSelected
                            }
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(5) { index ->
                                ChipItem(
                                    text = "Chip ${index + 1}",
                                    isSelected = (selectedIndex == index),
                                    unselectedBorderStroke = BorderStroke(
                                        color = Color(0xff167D5C),
                                        width = 1.dp
                                    ),
                                    onClick = {
                                        selectedIndex = index
                                    }
                                )
                            }
                        }

                        SectionTitle("PickerField Examples")

                        PickerField(
                            title = "Select Option",
                            text = pickerSelection,
                            hint = "Tap to choose",
                            onClick = {
                                pickerSelection = sampleOptions.random()
                            }
                        )

                        PickerField(
                            title = "Unavailable Field",
                            text = null,
                            hint = "Coming soon",
                            isEnabled = false,
                            errorMessage = "Currently unavailable",
                            onClick = {}
                        )

                        SectionTitle("CustomDatePicker Examples")

                        val selectedDateText = remember(selectedDate) {
                            selectedDate?.format(slashDateFormat)
                        }

                        CustomDatePicker(
                            title = "Select a future date",
                            selectedDate = selectedDate,
                            placeholderText = "Tap to choose",
                            targetDates = TargetDates.FUTURE,
                            selectedDateText = selectedDateText,
                            selectableDates = selectableDates,
                            onDateChange = { selectedDate = it },
                            isEnabled = false
                        )

                        SectionTitle("WheelDatePicker Examples")

                        WheelDatePicker(
                            title = "Select date with wheel",
                            selectedDate = wheelSelectedDate,
                            selectedDateText = wheelSelectedDate?.format(slashDateFormat),
                            targetDates = TargetDates.PAST,
                            isLabelsVisible = false,
                            onDateChange = { wheelSelectedDate = it }
                        )

                        SectionTitle("CustomDateRangePicker Examples")

                        val dateRangeText = remember(selectedStartDate, selectedEndDate) {
                            formatDateRange(selectedStartDate, selectedEndDate, slashDateFormat)
                        }

                        CustomDateRangePicker(
                            title = "Past Date Range",
                            rangeText = dateRangeText,
                            initialSelectedStartDate = selectedStartDate,
                            initialSelectedEndDate = selectedEndDate,
                            targetDates = TargetDates.PAST,
                            selectableDates = selectableDatesPast,
                            onRangeChange = { start, end ->
                                selectedStartDate = start
                                selectedEndDate = end
                            }
                        )

                        CustomDateRangePicker(
                            title = "Future Date Range",
                            rangeText = dateRangeText,
                            initialSelectedStartDate = selectedStartDate,
                            initialSelectedEndDate = selectedEndDate,
                            targetDates = TargetDates.FUTURE,
                            selectableDates = selectableDatesFuture,
                            enabledBorderWidth = 1.dp,
                            enabledBorderColor = Color(0xFF1976D2),
                            onRangeChange = { start, end ->
                                selectedStartDate = start
                                selectedEndDate = end
                            }
                        )

                        SectionTitle("OptionItem Multi-Select Dialog")

                        Text(
                            text = "Selected items: ${
                                selectedOptions.joinToString().ifBlank { "None" }
                            }",
                            style = CoreRegularTextStyle().copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        CustomButton(
                            text = "Show Multi-Select Dialog",
                            onClick = { showMultiSelectDialog = true }
                        )

                        SectionTitle("CustomHeader Examples")

                        CustomHeader(
                            onLeftIconClick = { },
                            centerIcon = Res.drawable.ic_cyclone
                        )

                        CustomHeader(
                            isRightIconButtonVisible = true,
                            rightIcon = Res.drawable.ic_cyclone,
                            rightIconDescription = "Right Action",
                            onLeftIconClick = { },
                            onRightIconClick = { }
                        )

                        CustomHeader(
                            backgroundColor = Color(0xFF1976D2),
                            leftIconTint = Color.White,
                            leftIconBackgroundColor = Color(0xFF0D47A1),
                            onLeftIconClick = { }
                        )

                        CustomHeader(
                            isLeftIconButtonVisible = false,
                            isRightIconButtonVisible = true,
                            rightIcon = Res.drawable.ic_cyclone,
                            rightIconDescription = "Menu",
                            onRightIconClick = { }
                        )

                        SectionTitle("CustomHeader - Trailing Slot Examples")

                        CustomHeader(
                            onLeftIconClick = { },
                            trailingSlot = {
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                            }
                        )

                        CustomHeader(
                            onLeftIconClick = { },
                            centerIcon = Res.drawable.ic_cyclone,
                            trailingSlot = {
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                            }
                        )

                        CustomHeader(
                            onLeftIconClick = { },
                            trailingSlot = {
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                            },
                            rightIcon = Res.drawable.ic_rotate_right,
                            onRightIconClick = { }
                        )

                        var showExtraOptions by remember { mutableStateOf(false) }
                        var isSelectionMode by remember { mutableStateOf(false) }

                        CustomHeader(
                            isCenterIconVisible = !isSelectionMode,
                            centerIcon = Res.drawable.ic_cyclone,
                            trailingSlot = if (showExtraOptions || isSelectionMode) {
                                {
                                    if (showExtraOptions) {
                                        CustomIconButton(
                                            icon = Res.drawable.ic_cyclone,
                                            onClick = { },
                                            iconDescription = ""
                                        )
                                        CustomIconButton(
                                            icon = Res.drawable.ic_cyclone,
                                            onClick = { },
                                            iconDescription = ""
                                        )
                                    }
                                    if (isSelectionMode) {
                                        CustomIconButton(
                                            icon = Res.drawable.ic_cyclone,
                                            onClick = {},
                                            iconDescription = ""
                                        )
                                    }
                                }
                            } else null,
                            rightIcon = if (!isSelectionMode) Res.drawable.ic_rotate_right else null,
                            onRightIconClick = {},
                            onLeftIconClick = { }
                        )

                        SectionTitle("CustomHeader - Leading Slot Examples")

                        CustomHeader(
                            leadingSlot = {
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                            }
                        )

                        CustomHeader(
                            onLeftIconClick = { },
                            leadingSlot = {
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                            }
                        )

                        CustomHeader(
                            onLeftIconClick = { },
                            leadingSlot = {
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                            },
                            centerIcon = Res.drawable.ic_cyclone,
                            trailingSlot = {
                                CustomIconButton(
                                    icon = Res.drawable.ic_cyclone,
                                    onClick = { },
                                    iconDescription = ""
                                )
                            },
                            rightIcon = Res.drawable.ic_rotate_right,
                            onRightIconClick = { }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomButton(
                                text = "Toggle Selection Mode",
                                onClick = { isSelectionMode = !isSelectionMode }
                            )
                            CustomButton(
                                text = "Toggle Extra Options",
                                onClick = { showExtraOptions = !showExtraOptions }
                            )
                        }

                        SectionTitle("CustomIconButton Examples")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CustomIconButton(
                                icon = Res.drawable.ic_cyclone,
                                iconDescription = "Forward",
                                onClick = { }
                            )

                            CustomIconButton(
                                buttonSize = 60.dp,
                                backgroundColor = Color(0xFF1976D2),
                                iconTint = Color.White,
                                shadowElevation = 6.dp,
                                icon = Res.drawable.ic_cyclone,
                                iconDescription = "Primary Forward",
                                onClick = { }
                            )
                        }

                        Text(
                            text = "Scrollbar Example",
                            style = CoreRegularTextStyle().copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        SectionTitle("OtpView Examples")

                        OtpView(
                            otpDigits = otpDigits,
                            onDigitChanged = { otpDigits = it },
                            otpSize = OtpSize.SIX,
                            requestFocusOnFirstDisplay = false
                        )

                        OtpView(
                            otpDigits = otpDigits4,
                            onDigitChanged = { otpDigits4 = it },
                            otpSize = OtpSize.FOUR,
                            requestFocusOnFirstDisplay = false
                        )

                        Text(
                            text = "OtpView — allowLetters = true (harf+rakam, harfler büyük; geçersiz karakter reddedilir)",
                            style = CoreRegularTextStyle().copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OtpView(
                            otpDigits = otpDigitsAlphanumeric,
                            onDigitChanged = { otpDigitsAlphanumeric = it },
                            otpSize = OtpSize.SIX,
                            requestFocusOnFirstDisplay = false,
                            allowLetters = true
                        )

                        OtpView(
                            otpDigits = otpDigitsError,
                            isError = isOtpError,
                            onDigitChanged = {
                                otpDigitsError = it
                                isOtpError =
                                    otpDigitsError.length == 6 && otpDigitsError != "123456"
                            },
                            otpSize = OtpSize.SIX,
                            requestFocusOnFirstDisplay = false
                        )

                        CustomButton(
                            text = "Trigger OTP Error",
                            onClick = { isOtpError = !isOtpError }
                        )

                        SectionTitle("ProgressIndicatorBars Examples")

                        ProgressIndicatorBars(
                            pageCount = 5,
                            currentPage = currentPage,
                            indicatorType = IndicatorType.HIGH_LIGHT_CURRENT
                        )

                        ProgressIndicatorBars(
                            pageCount = 5,
                            currentPage = currentPage,
                            indicatorType = IndicatorType.HIGH_LIGHT_UNTIL_CURRENT
                        )

                        ProgressIndicatorBars(
                            pageCount = 3,
                            currentPage = currentPage,
                            indicatorType = IndicatorType.HIGH_LIGHT_CURRENT,
                            filledIndicatorColor = Color(0xFFE91E63),
                            defaultIndicatorColor = Color(0xFFFFCDD2),
                            indicatorHeight = 10.dp,
                            indicatorSpacing = 5.dp
                        )

                        OptionItem(
                            text = "i read and i approve",
                            onItemClick = { clickOption },
                            backgroundColor = Color.Transparent
                        )

                        SectionTitle("ApprovalOptionItem")

                        OptionItem(
                            text = "okudum onaylıyorum",
                            boldLeadingText = "KVKK Aydınlatma Metni",
                            backgroundColor = Color.Transparent,
                            isRowClickEnabled = false,
                            onItemClick = {},
                            onCheckboxClick = {},
                            onBoldLeadingTextClick = {}
                        )

                        OptionItem(
                            text = "okudum kabul ediyorum",
                            boldLeadingText = "Gizlilik Sözleşmesi",
                            leadingIcon = Res.drawable.ic_cyclone,
                            backgroundColor = Color.Transparent,
                            isRowClickEnabled = false,
                            onItemClick = {},
                            onCheckboxClick = {},
                            onBoldLeadingTextClick = {}
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomButton(
                                text = "Previous",
                                enabled = currentPage > 0,
                                onClick = { if (currentPage > 0) currentPage-- }
                            )
                            CustomButton(
                                text = "Next",
                                enabled = currentPage < 4,
                                onClick = { if (currentPage < 4) currentPage++ }
                            )
                        }
                    }
                }

                items(30) { index ->
                    RoundedSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Scrollable item #$index",
                            modifier = Modifier.padding(16.dp),
                            style = CoreMediumTextStyle().copy(fontSize = 16.sp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            OptionItemLazyListDialog(
                optionsList = sampleOptions,
                optionText = { it },
                optionId = { it.hashCode() },
                isDialogVisible = showOptionDialog,
                isScrollBarVisible = false,
                selectedOption = selectedOption,
                onOptionItemClick = {
                    selectedOption = it
                    showOptionDialog = false
                },
                onDismissRequest = { showOptionDialog = false }
            )

            OptionItemMultiSelectLazyListDialog(
                optionsList = sampleOptions,
                optionText = { it },
                optionId = { it.hashCode() },
                isDialogVisible = showMultiSelectDialog,
                selectedOptions = selectedOptions.toList(),
                onOptionItemClick = { item ->
                    selectedOptions = if (selectedOptions.contains(item)) {
                        selectedOptions - item
                    } else {
                        selectedOptions + item
                    }
                },
                onSaveButtonClick = { showMultiSelectDialog = false },
                onDismissRequest = { showMultiSelectDialog = false }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = CoreSemiBoldTextStyle().copy(
            fontSize = 18.sp,
            color = Color(0xFF1976D2)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun QrGeneratorDemoScreen(onBack: () -> Unit) {
    val data = "https://deveng.global/"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "CustomQrCodeView",
                style = CoreBoldTextStyle().copy(fontSize = 22.sp),
            )
            Text(
                text = data,
                style = CoreRegularTextStyle().copy(
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomQrCodeView(
                data = data,
                modifier = Modifier.size(280.dp),
                contentDescription = "QR code linking to deveng.global",
                overlayFraction = 0.4f,
                overlayContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1861f / 590f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.deveng_logo),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onBack) { Text("Back") }
        }
    }
}

/**
 * Demo screen that exercises the deveng-core QR scanner. Uses the library's
 * [PermissionsController] for a clean suspend-based permission flow that works consistently
 * on Android, iOS, Desktop, and WASM (where the browser handles the actual prompt inside
 * `getUserMedia` and the controller is optimistic).
 */
@Composable
private fun QrScannerDemoScreen(onBack: () -> Unit) {
    val factory = rememberPermissionsControllerFactory()
    val permissionsController = remember(factory) { factory.createPermissionsController() }
    BindEffect(permissionsController)

    var permissionState by remember { mutableStateOf(PermissionState.NotDetermined) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var requestAttempt by remember { mutableStateOf(0) }

    LaunchedEffect(permissionsController, requestAttempt) {
        try {
            val current = permissionsController.getPermissionState(Permission.CAMERA)
            if (current == PermissionState.Granted) {
                permissionState = PermissionState.Granted
            } else {
                permissionsController.providePermission(Permission.CAMERA)
                permissionState = PermissionState.Granted
            }
        } catch (e: DeniedAlwaysException) {
            permissionState = PermissionState.DeniedAlways
        } catch (e: DeniedException) {
            permissionState = PermissionState.Denied
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
            permissionState = PermissionState.Denied
        }
    }

    var lastScannedCode by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (permissionState) {
            PermissionState.Denied,
            PermissionState.DeniedAlways,
            PermissionState.NotGranted -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = errorMessage
                            ?: "Camera permission is required to scan QR codes.",
                        color = Color.White,
                        style = CoreRegularTextStyle().copy(fontSize = 16.sp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (permissionState == PermissionState.DeniedAlways) {
                            Button(onClick = { permissionsController.openAppSettings() }) {
                                Text("Open settings")
                            }
                        } else {
                            Button(onClick = { requestAttempt++ }) { Text("Try again") }
                        }
                        Button(onClick = onBack) { Text("Back") }
                    }
                }
            }

            PermissionState.NotDetermined -> {
                Box(
                    Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = "Requesting camera permission...",
                            color = Color.White,
                            style = CoreRegularTextStyle().copy(fontSize = 16.sp),
                        )
                    }
                }
            }

            PermissionState.Granted -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    QrScannerView(
                        onQrScanned = { code -> lastScannedCode = code },
                        scannerConfig = QrScannerConfig(scanThrottleMs = 1500L),
                        modifier = Modifier.fillMaxSize(),
                    )

                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                    ) { Text("Back") }

                    lastScannedCode?.let { code ->
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Scanned:",
                                color = Color.White.copy(alpha = 0.7f),
                                style = CoreRegularTextStyle().copy(fontSize = 12.sp),
                            )
                            Text(
                                text = code,
                                color = Color.White,
                                style = CoreRegularTextStyle().copy(fontSize = 14.sp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
