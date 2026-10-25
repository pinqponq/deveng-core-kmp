# Deveng Core KMP

Kotlin Multiplatform core library: shared UI, utilities, data, and permissions for Android, iOS, Desktop, and Web.

## Overview

Deveng Core KMP is a KMP library that provides:

- **UI & theming** — Compose components (buttons, dialogs, pickers, navigation, OTP, option lists, etc.) with a customizable theme and typography system.
- **Data & domain** — Key-value storage (e.g. device identifier); **generic temp file storage** (repository interface + impl, `TempFileItem`, `TempStorageDirProvider`, `TempFileOps` in `core.domain.temp` / `core.data.temp`); pagination models and loaders; camera/capture (`SharedImage`, `CameraManager`) with expect/actual per platform.
- **Utilities** — Platform detection and config, date/time formatting, image resize/compress, string formatting, logging, dispatchers, clipboard, dial, maps, share, location.
- **Permissions** — Unified permission API (camera, gallery, location, Bluetooth, contacts, notifications, motion) with Android and iOS implementations.

## Supported Platforms

| Platform | Support |
|----------|---------|
| Android  | ✅      |
| iOS      | ✅ (device + simulator) |
| Desktop  | ✅ (JVM) |
| Web      | ✅ (Wasm/JS) |

## Installation

Add the dependency to your module (Kotlin DSL):

```kotlin
dependencies {
    implementation("global.deveng:core-kmp:VERSION")
}
```

Replace `VERSION` with the [latest release](https://central.sonatype.com/artifact/global.deveng/core-kmp) or the version from the repo.

## Table of Contents

- [Quick Start](#quick-start)
- [Library Modules](#library-modules)
- [UI Components & Theming](#ui-components--theming)
- [Utilities](#utilities)
- [Data & Domain](#data--domain)
- [Camera](#camera)
- [Permissions](#permissions)
- [Push Notifications](#push-notifications)
- [Auth Session](#auth-session)
- [Pagination](#pagination)
- [BaseViewModel and interceptors](#baseviewmodel-and-interceptors)
- [Client Logs (deveng-pinqloq)](#client-logs-deveng-pinqloq)
- [File picker (deveng-filepicker)](#file-picker-deveng-filepicker)
- [Web analytics (Google Analytics 4)](#web-analytics-google-analytics-4)
- [Sample App](#sample-app)
- [Building](#building)
- [License](#license)

## Quick Start

**UI** — Wrap your composables in `AppTheme` and use shared components:

```kotlin
AppTheme {
    CustomButton(text = "Click me", onClick = { })
}
```

**Platform & utils** — Use `MultiPlatformUtils` (construct with `Context` on Android; no-arg on other platforms) for platform config and common actions:

```kotlin
val utils = MultiPlatformUtils(context) // Android; use no-arg constructor on iOS/Desktop/Wasm
val config = utils.getPlatformConfig() // platform, language, uuid, deviceName, packageVersionName
utils.copyToClipBoard("text")
utils.openUrl("https://example.com")
```

## Library Modules

The `deveng-core` module is organized as follows:

| Area | Description |
|------|-------------|
| **UI & theming** | `ComponentTheme`, `AppTheme`, typography; buttons, dialogs, text fields, date/range pickers, OTP, navigation menus, option lists, chips, search, progress, scrollbars, JSON viewer, etc. |
| **Data** | `DeviceInfoStorage` (e.g. device identifier); **generic temp file storage** — `TempFileRepository`, `TempFileRepositoryImpl`, `TempStorageDirProvider`, `TempFileOps`, `TempFileItem` (domain in `core.domain.temp`, impl in `core.data.temp`); use one `TempStorageDirProvider` per use case (e.g. camera path); `PagedListResponse`, `BasePaginatedResponse` for paginated API responses. |
| **Domain** | `PagedList` for pagination; camera (see [Camera](#camera)) — `rememberCameraKState`, `DefaultCameraPreview`, `CameraPreviewView`, `ImageCaptureResult.Success(byteArray, bitmap)`; image save & EXIF via `core.util.image.PhotoSaveUtils`. |
| **Utilities** | `Platform`, `PlatformConfig`, `MultiPlatformUtils` (dial, clipboard, open URL, maps, share, location); date/time formatting; `ImageSizeProcessor` and `ImageProcessingProfile`; `StringFormatter`, `CustomLogger`, `CustomDispatchers`. |
| **Permissions** | `Permission` enum, `PermissionsController`, `PermissionState`; Android and iOS implementations. |
| **Push notifications** | `PushNotifications` (permission status, soft-ask cadence, system prompt, token registration), `PushNotificationMessagingService` (Android), `IosPushTokenBridge` (iOS); see [Push Notifications](#push-notifications). |
| **Pagination (UI)** | `PaginatedFlowLoader`, `PaginatedListView`, page state. |

## UI Components & Theming

### Step-by-Step Theming Guide

#### Step 1: Basic Usage (Default Theme)

Components work out of the box with default colors and Urbanist font:

```kotlin
AppTheme {
    CustomButton(text = "Button", onClick = { })
    CustomAlertDialog(
        isDialogVisible = true,
        title = "Title",
        description = "Description",
        positiveButtonText = "OK",
        onPositiveButtonClick = { }
    )
}
```

#### Step 2: Customize Colors

Create a `ComponentTheme` to override component colors:

```kotlin
val theme = ComponentTheme(
    button = ButtonTheme(
        containerColor = Color.Blue,
        contentColor = Color.White
    ),
    alertDialog = AlertDialogTheme(
        headerColor = Color.White,
        titleColor = Color.Black
    )
)

AppTheme(componentTheme = theme) {
    // Components use your custom colors
}
```

#### Step 3: Customize Font Family

Change the default font (Urbanist) globally:

```kotlin
// System fonts
val theme = ComponentTheme(
    typography = TypographyTheme(
        fontFamily = FontFamily.SansSerif
    )
)

// Custom font files
val customFont = FontFamily(
    Font(resource = Res.font.my_font_regular, weight = FontWeight(400)),
    Font(resource = Res.font.my_font_bold, weight = FontWeight(700))
)

val theme = ComponentTheme(
    typography = TypographyTheme(fontFamily = customFont)
)
```

#### Step 4: Override at Component Level

Override theme values for specific components:

```kotlin
AppTheme(componentTheme = theme) {
    CustomButton(
        text = "Special Button",
        containerColor = Color.Red, // Overrides theme
        textStyle = BoldTextStyle().copy(fontSize = 20.sp)
    )
}
```

### Theme Structure

```
ComponentTheme
├── typography: TypographyTheme
│   └── fontFamily: FontFamily? (default: Urbanist)
├── button: ButtonTheme
│   ├── containerColor, contentColor
│   ├── disabledContainerColor, disabledContentColor
│   └── defaultTextStyle
├── alertDialog: AlertDialogTheme
│   ├── headerColor, bodyColor
│   ├── titleColor, descriptionColor
│   ├── positiveButtonColor, negativeButtonColor
│   └── titleTextStyle, descriptionTextStyle, buttonTextStyle
├── surface: SurfaceTheme
│   └── defaultColor, defaultContentColor
└── dialogHeader: DialogHeaderTheme
    └── titleColor, iconTint, titleTextStyle
```

### Typography

**Available functions:** `RegularTextStyle()` (400), `MediumTextStyle()` (500), `SemiBoldTextStyle()` (600), `BoldTextStyle()` (700). All use the font family from `ComponentTheme.typography.fontFamily` (default: Urbanist).

```kotlin
Text(text = "Text", style = RegularTextStyle())
Text(text = "Custom", style = BoldTextStyle().copy(fontSize = 24.sp, color = Color.Blue))
CustomButton(text = "Button", textStyle = SemiBoldTextStyle().copy(fontSize = 18.sp), onClick = { })
```

`CustomButton` keeps Material's inner padding (24.dp horizontal, 8.dp vertical) unless you pass `contentPadding`. A narrow or short button can give its label more room this way:

```kotlin
CustomButton(
    modifier = Modifier.width(120.dp).height(40.dp),
    text = "See answer",
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    onClick = { }
)
```

### Components (overview)

- **Actions:** `CustomButton`, `CustomIconButton`, `LabeledSwitch`
- **Dialogs:** `CustomAlertDialog`, `CustomDialog`, `CustomDialogHeader`, `CustomDialogBody`
- **Inputs:** `CustomTextField`, `SearchField`, `PickerField`, `CustomDatePicker`, `CustomDateRangePicker`, `OtpView`
- **Layout:** `CustomHeader`, `RoundedSurface`, `LabeledSlot`, `Slot`, `ChipItem`
- **Lists & selection:** `OptionItemList`, `OptionItemLazyListDialog`, `OptionItemMultiSelectLazyListDialog`, `CustomDropDownMenu`
- **Navigation:** `NavigationMenu` (horizontal, expanded, collapsed, bottom bar, side bar; see [NavigationMenu bar modes](#navigationmenu-bar-modes))
- **Feedback:** `RatingRow`, `ProgressIndicatorBars`, `JsonViewer`, `CustomQrCodeView` (see [CustomQrCodeView](#customqrcodeview))
- **Swipe stack:** `SwipeCards`, `SwipeCardsState`, `rememberSwipeCardsState`, `SwipeCardsScope`; optional left/right/revert icon buttons; use `pendingRevertKey` when the list updates so the revert button stays visible (see [SwipeCards](#swipecards))
- **Scrolling:** `PaginatedListView`, `ScrollbarWithScrollState`, `ScrollbarWithLazyListState`

### Complete UI Example

```kotlin
@Composable
fun MyApp() {
    val theme = ComponentTheme(
        typography = TypographyTheme(fontFamily = FontFamily.SansSerif),
        button = ButtonTheme(
            containerColor = Color(0xFF1976D2),
            contentColor = Color.White,
            defaultTextStyle = SemiBoldTextStyle().copy(fontSize = 18.sp)
        ),
        alertDialog = AlertDialogTheme(
            headerColor = Color.White,
            titleColor = Color.Black,
            titleTextStyle = BoldTextStyle().copy(fontSize = 20.sp)
        )
    )

    AppTheme(componentTheme = theme) {
        Column {
            CustomButton(text = "Primary Action", onClick = { })
            CustomAlertDialog(
                isDialogVisible = showDialog,
                title = "Confirm",
                description = "Are you sure?",
                positiveButtonText = "Yes",
                negativeButtonText = "No",
                onPositiveButtonClick = { showDialog = false },
                onNegativeButtonClick = { showDialog = false },
                onDismissRequest = { showDialog = false }
            )
        }
    }
}
```

### SwipeCards

Tinder-style swipeable card stack: `SwipeCards`, `SwipeCardsState`, `rememberSwipeCardsState`, `SwipeCardsScope` (`items` / `itemsIndexed`). Use `showSwipeButtons` with `negativeButtonIcon`, `positiveButtonIcon`, `revertButtonIcon` for overlay buttons. Revert is normally driven by internal state (`state.canRevert`, `popLastSwipe`, `animateBackSwipe`). When the **list content changes** (e.g. you remove an item after committing a previous swipe), the internal revert state is lost and the built-in revert button would disappear. Pass **`pendingRevertKey`** with the pending item's key (same id you use in `items`/`itemsIndexed`) so the revert button stays visible and a click calls `onRevert(pendingRevertKey)`.

### NavigationMenu bar modes

`MenuMode.BottomBar` renders a compact bar at the bottom; `MenuMode.SideBar` renders the same bar as a narrow column at the start or end (`MenuAlignment`). Both take an optional sliding `NavigationMenuIndicator` (color, shape, inset or fixed size, animation spec) and a `barItemContent` slot to draw an item differently (e.g. a center logo). `isBarItemRippleEnabled = false` turns off the press ripple of the bar items (default: on). `NavigationBarPlacement` picks bottom vs. side from the window size (`currentNavigationBarPlacement()`, built on `currentDeviceScreenType()`), and `contentPadding(barThickness)` keeps screen content clear of the bar.

```kotlin
val placement = currentNavigationBarPlacement()

Box(modifier = Modifier.fillMaxSize()) {
    AppContent(modifier = Modifier.padding(placement.contentPadding(barThickness = 54.dp)))

    NavigationMenu(
        isExpanded = false,
        menuMode = placement.menuMode,
        menuAlignment = placement.menuAlignment,
        modifier = Modifier.align(if (placement == NavigationBarPlacement.Bottom) Alignment.BottomCenter else Alignment.CenterStart),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        barThickness = 54.dp,
        barIndicator = NavigationMenuIndicator(color = Color(0xFF262626)),
        barItemContent = { tab, isSelected ->
            if (tab.isCenter) {
                Image(painter = painterResource(tab.icon), contentDescription = tab.title)
            } else {
                NavigationMenuBarItemIcon(icon = tab.icon, iconTint = if (isSelected) selectedColor else unselectedColor, contentDescription = tab.title)
            }
        },
        itemList = tabs,
        isItemSelected = { tab -> tab == selectedTab },
        itemText = { tab -> tab.title },
        itemTextStyle = { TextStyle() },
        itemIcon = { tab -> tab.icon },
        itemIconTint = { unselectedColor },
        itemIconDescription = { tab -> tab.title },
        onItemClick = { tab -> selectedTab = tab }
    )
}
```

Theme defaults live in `NavigationMenuTheme` (`barThickness`, `barShape`, `barItemIconSize`). The sample app's "Navigation Bar Demo" shows the bar at the bottom and on both sides.

### CustomQrCodeView

Multiplatform QR code generator backed by [QRose](https://github.com/alexzhirkevich/qrose); works on Android, iOS, Desktop JVM and WASM/JS from `commonMain`.

```kotlin
CustomQrCodeView(
    data = "https://deveng.global/",
    modifier = Modifier.size(280.dp),
    contentDescription = "QR code linking to deveng.global",
    overlayContent = {
        Image(painter = painterResource(Res.drawable.deveng_logo), contentDescription = null)
    }
)
```

**Defaults vs overrides.** Global defaults live in `QrCodeViewTheme` (colors, overlay fraction, and module shapes) and can be set on the app's `ComponentTheme`. Every theme value also has a nullable per-call override on the composable so a single QR can deviate from the global look without touching the theme.

**Error correction.** The `errorCorrectionLevel` parameter defaults to `QrErrorCorrectionLevel.High` (~30% damage tolerance) so that codes with a centered logo overlay still scan reliably. Lower it to `Medium` or `Auto` when there is no overlay and you want a tidier, less dense code.

**Overlay fraction.** `overlayFraction` (theme default `0.25f`) controls the share of the QR's width that the centered `overlayContent` occupies. Stay at or below ~0.30 to leave a safety margin at level High; ~0.40 still scans in clean conditions but with no margin.

**Shape knobs.** `pixelShape` / `ballShape` / `frameShape` (theme defaults: square) accept any `QrPixelShape` / `QrBallShape` / `QrFrameShape` produced by the QRose factories — for example `QrPixelShape.circle()`, `QrBallShape.roundCorners(0.25f)`, `QrFrameShape.roundCorners(0.5f)` — for a styled look.

```kotlin
AppTheme(
    componentTheme = ComponentTheme(
        qrCodeView = QrCodeViewTheme(
            darkColor = Color(0xFF0F172A),
            pixelShape = QrPixelShape.circle(),
            ballShape = QrBallShape.circle(),
            frameShape = QrFrameShape.roundCorners(0.25f),
        )
    )
) { /* app content */ }
```

When you use `overlayContent`, prefer a backdrop sized to your logo's natural aspect ratio (a rounded pill or circle) rather than filling the whole overlay square — this hides only the QR pixels actually under the logo and reads cleaner. See the sample app's `QrGeneratorDemoScreen` for a worked example.

### Atmosphere: CRT screen, swirl background and vortex

Package `core.presentation.effect` (runtime shaders: AGSL on Android 13+, SkSL on iOS, desktop and web; ports of Balatro's CRT and background shaders):

| Need | Use |
|------|-----|
| Old TV look over a screen (curvature, scanlines, color fringing, grain, bloom, vignette) | `Modifier.crtScreen(CrtConfig())` on a container that paints an opaque background |
| Slowly moving paint swirl in three colors | `SwirlBackground(SwirlPalette(base, accent, shade), spinAmount, contrast)` |
| Short vortex and flash between two stages | `VortexFlash(progress, palette)` with `progress` 0 to 1 over the change |

Below Android 13 `crtScreen` draws plain scanlines and a vignette and `SwirlBackground` two soft moving gradients; nothing crashes. While `LocalReduceMotion` is on, the grain and the swirl stand still. The sample app's "Atmosphere Demo" shows all three.

### Motion, press feedback, shimmer and haptics

Package `core.presentation.motion` (plus `core.presentation.haptics` and `core.presentation.component.shimmer`):

| Need | Use |
|------|-----|
| Shared durations, easings, springs | `MotionTokens` (durations `DURATION_MICRO_MILLIS` 50 / `QUICK` 100 / `SHORT` 150 / `REGULAR` 200 / `MEDIUM` 300 / `LONG` 500, `StandardEasing`, `AccelerateDecelerateEasing`, `DecelerateEasing`, `DistanceScaledEndingEasing`, `EmphasizedDecelerateEasing`, `distanceScaledDurationMillis(remainingFraction)`, `defaultSpring()`, `bouncySpring()`, `pressSpring()`, `tweenOrSnap(isReduceMotionEnabled)`) |
| Scale down slightly while pressed | `Modifier.pressScale(interactionSource)`; pass the same `interactionSource` to `clickable` |
| Loading placeholder | `ShimmerBox(modifier)` or `Modifier.shimmer()`; colors and corner radius from `ComponentTheme.shimmer` (`ShimmerTheme`) |
| Screen transitions | `NavTransitions.enter` / `exit` / `popEnter` / `popExit(isReduceMotionEnabled)` for NavHost or `AnimatedContent` |
| System reduce-motion setting | `LocalReduceMotion` (provided by `AppTheme`, override with `AppTheme(reduceMotion = …)`); Android: "Remove animations", iOS: Reduce Motion, desktop/web: false |
| Haptics | `rememberAppHaptics()` → `tap()`, `success()`, `warning()` (Compose `VirtualKey` / `Confirm` / `Reject`) |
| Short grow-and-settle bounce ("pop") | `rememberJuiceState()` + `Modifier.juice(state)`; call `state.pop()`. A new pop restarts the running one |
| Card that turns between two faces | `FlipCard(front, back, isFlipped, delayMillis = …)`: `rotationY` with a camera distance, each face composed only while visible; a small different `delayMillis` per card keeps several cards from moving in lockstep |
| Number that counts up step by step | `CountUpText(value, startValue, formatter, juiceState, onStep)`: at most `maxStepCount` steps and `maxTotalMillis` in total, `onStep` per step |
| Haptic ticks of three strengths | `rememberHapticTicker()` returns a ticker with `light()` (dropped when faster than `minLightIntervalMillis`), `medium()`, `strong()` |

Everything that animates respects `LocalReduceMotion`: `pressScale` stays at full size, `shimmer` shows the static base color, `NavTransitions` return no transition, `pop()` does nothing, `FlipCard` swaps faces at once and `CountUpText` shows the final number at once. Defaults of the pop, flip and count-up are in `MotionDefaults`. `distanceScaledDurationMillis` gives a gesture ending that follows the distance left: `max(maxMillis × remainingFraction, minMillis)`, with the snap-back (320 / 120 ms) and finish (200 / 50 ms) values as defaults; pair it with `DistanceScaledEndingEasing`. The sample app's "Motion Demo" shows the 50 to 300 ms durations and the new easings.

```kotlin
val isReduceMotionEnabled = LocalReduceMotion.current
NavHost(
    navController = navController,
    startDestination = Home,
    enterTransition = { NavTransitions.enter(isReduceMotionEnabled) },
    exitTransition = { NavTransitions.exit(isReduceMotionEnabled) },
    popEnterTransition = { NavTransitions.popEnter(isReduceMotionEnabled) },
    popExitTransition = { NavTransitions.popExit(isReduceMotionEnabled) },
) { /* … */ }
```

The sample app's **Motion Demo** screen shows all of them; the "Reveal motion" section there has the pop, the flip card and the count-up.

### Best Practices (UI)

1. **Define theme once** — Create `ComponentTheme` at app level.
2. **Use theme for consistency** — Override only when needed.
3. **Font hierarchy** — Titles: 20–24sp (Bold), Body: 14–16sp (Regular), Buttons: 16–18sp (SemiBold).
4. **Accessibility** — Ensure sufficient color contrast and readable font sizes.
5. **Custom fonts** — Include all weights (400, 500, 600, 700) your app uses.

---

### Server-driven UI

`ServerDrivenEngine` + `ServerDrivenScreen` (`core.serverdriven`) draw a layout the backend sends as JSON, with
values, visibility/editable dependencies and lazily loaded shutter content. A layout carries an optional `version`
(absent = 1) and `onLayoutElementSkip` reports each unknown component type, operator or effect the engine skips,
once per layout, for analytics. Component types: `shutter`, `tabs`, `row`, `section`, `label`, `divider`, `slider`,
`switch`, `textfield`, `dropdown`, `segmented`, `radio`, `rating` and `date`. Format, rules and usage:
[docs/server-driven-ui.md](docs/server-driven-ui.md). The sample app's "Server-driven UI Demo"
(`ServerDrivenDemoScreen`) draws a JSON layout with every component type, a visibility and an editable dependency, the
design document's day/night tabs (ids only need to be unique inside one tab), a shutter whose content loads on first
open, a feedback section with the label, segmented, radio, rating and date components and their dependencies, and a
component of a type the library does not know, which is skipped and reported under the layout.

## Utilities

**Platform & actions** — `MultiPlatformUtils` (use `Context` on Android; no-arg on iOS/Desktop/Wasm):

```kotlin
val utils = MultiPlatformUtils(context)
val config = utils.getPlatformConfig() // platform, systemLanguage, uuid, deviceName, packageVersionName
utils.copyToClipBoard("Copied text")
utils.openUrl("https://example.com")
utils.openMapsWithLocation(41.0082, 28.9784)
utils.shareText("Share this")
val (lat, lng) = utils.getCurrentLocation() ?: (0.0 to 0.0)
```

**Date/time** — Format and selectable dates:

```kotlin
val formatted = formatDateTime(localDateTime, isDaily = false)  // e.g. "20.02.2025"
val dateString = localDate.format(dotLocalDateFormat)
val selectableDates = CustomSelectableDates(...)  // for date picker constraints
```

**Image processing** — Resize and compress image bytes:

```kotlin
val processor = ImageSizeProcessor()
val compressed = processor.resizeAndCompressBytes(
    inputBytes = imageBytes,
    profile = ImageProcessingProfile.MEDIUM  // or targetMaxSizePx + quality
)
val bitmap = imageBytes.toImageBitmap()
```

**String & logging**:

```kotlin
val cleaned = StringFormatter().formatInput(input, clearNonNumeric = true)
CustomLogger.isLoggingEnabled = true
CustomLogger.log("Debug message")
```

**Markdown** — `MarkdownContentParser` (`core.util.markdown`) renders a markdown string line by line with a caller-controlled text color. It supports headings (`#` to `######`), list items (`-`, `*` or `+`, indented for nesting; numbered items keep their number), fenced code blocks (```` ``` ````, scrolled sideways instead of wrapped), inline code, bold (`**text**`), italic (`_text_` or `*text*`; a delimiter inside a word such as `snake_case_name` stays plain text), links (`[text](url)`) and bare `http`/`https` URLs. Tapping a link opens it with the platform `LocalUriHandler`. The sample app shows it in "Show Markdown Dialog".

```kotlin
MarkdownContentParser(
    markdownContent = reply,
    textColor = MaterialTheme.colorScheme.onSurface,
    textStyle = CoreRegularTextStyle().copy(fontSize = 15.sp),     // optional: font family, size, line height
    linkColor = MaterialTheme.colorScheme.primary,                 // optional, defaults to textColor
    codeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant // optional, defaults to textColor at 12% alpha
)
```

**JWT claims** — Read the claims of a JWT on the client (`core.util`). The payload is decoded as base64url with or without padding. The signature is not verified, so never use the claims to make trust decisions.

```kotlin
@Serializable
data class AccessTokenClaims(
    @SerialName("sub")
    val subject: String
)

val payloadJson = decodeJwtPayload(token)              // raw JSON payload
val claims = decodeJwtClaims<AccessTokenClaims>(token) // unknown claims are ignored
val expiresAt: Instant? = jwtExpiresAt(token)          // standard "exp" claim, null when absent
```

All three throw `IllegalArgumentException` when the token does not have three parts or the payload is not valid (`SerializationException` for JSON errors is a subclass). The sample app shows them in "Open JWT Claims Demo".

---

## Data & Domain

**Device storage** — Persist a generated device identifier:

```kotlin
val storage = DeviceInfoStorageImpl(settings)  // Settings from russhwolf/settings
var uuid = storage.getGeneratedPlatformIdentifier()
if (uuid == null) {
    uuid = UUID.randomUUID().toString()
    storage.setGeneratedPlatformIdentifier(uuid)
}
```

**Pagination** — Map API response to domain model:

```kotlin
@Serializable
data class MyItem(val id: String, val name: String)

val response: PagedListResponse<MyItemDto> = api.getPage(page, size)
val pagedList = response.mapItems { dto -> MyItem(dto.id, dto.name) }
// pagedList.items, pagedList.hasNextPage, pagedList.totalPageCount, etc.
```

**Camera** — See [Camera](#camera) for photo capture and video recording with the core module.

---

## Camera

The core module provides a full camera implementation for **Android**, **iOS**, and **Desktop** (Compose-first API). Capture returns in-memory bytes and an optional bitmap; the **app** is responsible for saving the photo (including adding location EXIF and notifying the Gallery on Android). On **WASM/JS** the camera is unsupported (no-op / error state).

### Quick implementation guide

**Step 1 — Camera UI and capture**

Use `rememberCameraKState` and `DefaultCameraPreview` for a ready-to-use camera screen (flash, switch camera, zoom, tap-to-focus, capture button, thumbnail). Capture does **not** write to disk; you receive `ImageCaptureResult.Success(byteArray, bitmap)`.

```kotlin
import core.domain.camera.compose.rememberCameraKState
import core.domain.camera.compose.DefaultCameraPreview
import core.domain.camera.state.CameraConfiguration
import core.domain.camera.state.CameraKState
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.AspectRatio
import core.domain.camera.result.ImageCaptureResult
import core.util.image.PhotoSaveUtils
import core.util.image.SavePhotoResult

@Composable
fun CameraScreen() {
    val config = remember {
        CameraConfiguration(
            cameraLens = CameraLens.BACK,
            flashMode = FlashMode.OFF,
            aspectRatio = AspectRatio.RATIO_16_9,
        )
    }

    val cameraState by rememberCameraKState(config = config, setupPlugins = { })

    when (val state = cameraState) {
        is CameraKState.Initializing -> CircularProgressIndicator()
        is CameraKState.Ready -> {
            DefaultCameraPreview(
                controller = state.controller,
                onImageCaptured = { result ->
                    when (result) {
                        is ImageCaptureResult.Success -> {
                            // Step 2: add location EXIF (optional), then save — see below
                            saveCapturedPhoto(result.byteArray, result.bitmap)
                        }
                        is ImageCaptureResult.Error ->
                            println("Error: ${result.exception.message}")
                        else -> { }
                    }
                },
                onGalleryClick = { /* open gallery */ },
                onLastPhotoClick = { bitmap -> /* open viewer */ },
                initialThumbnailBitmap = null,
            )
        }
        is CameraKState.Error -> Text("Camera error: ${state.message}")
    }
}
```

**Step 2 — Save photo and optional location EXIF**

Use **`PhotoSaveUtils`** (`core.util.image`) to add GPS EXIF and write the file. On Android, call **`PhotoSaveUtils.setApplicationContext(applicationContext)`** once (e.g. in `Activity.onCreate`) so saved photos are notified to MediaStore and appear in the system Gallery.

```kotlin
// In Activity.onCreate (Android only):
PhotoSaveUtils.setApplicationContext(applicationContext)

// When you have capture bytes and optional location:
fun saveCapturedPhoto(byteArray: ByteArray, thumbnail: ImageBitmap?) {
    val path = getNewPhotoSavePath()  // your app provides the path, e.g. Pictures/MyApp/IMG_xxx.jpg

    val (lat, lon) = getCurrentLocation() ?: (null to null)
    val bytesToSave = if (lat != null && lon != null) {
        PhotoSaveUtils.addLocationExif(byteArray, lat, lon)
    } else {
        byteArray
    }

    when (val saveResult = PhotoSaveUtils.savePhoto(bytesToSave, path)) {
        is SavePhotoResult.Success -> println("Saved: ${saveResult.path}")
        is SavePhotoResult.Error -> println("Save failed: ${saveResult.exception.message}")
    }
}
```

**Camera temp storage (stack / pre-upload)**

To persist captured photos in temp storage (e.g. for a stack-and-swipe flow before upload), use **`CameraTempPhotoRepository`** (`core.domain.camera.temp`) and **`TempPhotoItem`**. Register **`CameraTempDirProvider`** and **`CameraTempFileOps`** in your DI (Koin); core provides default implementations per platform (`AndroidCameraTempDirProvider`, `DesktopCameraTempDirProvider`, `IosCameraTempDirProvider`). Use **`PhotoSaveUtils.imageBytesWithNormalOrientation(byteArray)`** before saving so orientation is correct. The repository interface lives in **core.domain.camera.temp**; the implementation and file/dir abstractions in **core.data.camera.temp** (domain + data split).

**Step 3 — Provide the save path**

Your app decides where to save (e.g. app-specific directory or public Pictures). Example for a path under Pictures:

```kotlin
// Example: platform-specific function that returns a new file path
expect fun getNewPhotoSavePath(): String
// Android actual: e.g. Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES) + "/MyApp/IMG_${timestamp}.jpg"
```

### Image saving and EXIF (PhotoSaveUtils)

| API | Description |
|-----|-------------|
| **`PhotoSaveUtils.setApplicationContext(context: Any?)`** | Call once on Android with application context so saved photos are scanned and appear in the system Gallery. No-op on other platforms. |
| **`PhotoSaveUtils.savePhoto(imageBytes, targetPath): SavePhotoResult`** | Writes JPEG/PNG bytes to `targetPath`, creates parent directories. Returns `SavePhotoResult.Success(path)` or `SavePhotoResult.Error(exception)`. |
| **`PhotoSaveUtils.addLocationExif(imageBytes, latitude, longitude): ByteArray`** | Returns a new byte array with GPS EXIF tags set (decimal degrees). Preserves existing EXIF (e.g. orientation). Implemented on Android; other platforms return bytes unchanged. |

Flow: **capture** → **addLocationExif** (if you have location) → **savePhoto** → optionally use thumbnail from `ImageCaptureResult.Success.bitmap` for UI.

### Thumbnail and gallery icon

- **Thumbnail** (bottom-left): Shows the last captured photo or an optional **`initialThumbnailBitmap`**. Tapping it calls **`onLastPhotoClick(bitmap)`** with the displayed bitmap.
- **Gallery icon**: Shown only when there is no thumbnail (no initial bitmap and no capture yet).

### Custom UI with CameraPreviewView

For a custom layout, use `CameraPreviewView` and call the controller:

```kotlin
when (val state = cameraState) {
    is CameraKState.Ready -> {
        val controller = state.controller
        CameraPreviewView(controller = controller, modifier = Modifier.fillMaxSize())

        Button(
            onClick = {
                scope.launch {
                    when (val result = controller.takePictureToFile()) {
                        is ImageCaptureResult.Success ->
                            saveCapturedPhoto(result.byteArray, result.bitmap)
                        is ImageCaptureResult.Error ->
                            println("Error: ${result.exception.message}")
                        else -> { }
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Text("Capture")
        }
    }
    // ...
}
```

### Capture result

- **`takePictureToFile()`** — Returns **`ImageCaptureResult.Success(byteArray, bitmap)`**. Does not write to disk; the app saves using `PhotoSaveUtils.savePhoto`.
- **`ImageCaptureResult.Success`** — `byteArray`: JPEG/PNG bytes; `bitmap`: optional decoded bitmap for thumbnails. Use `PhotoSaveUtils.addLocationExif` and `PhotoSaveUtils.savePhoto` to persist.

### Platform setup

| Platform | Setup |
|----------|--------|
| **Android** | Camera and storage permissions in core's manifest. Call `PhotoSaveUtils.setApplicationContext(applicationContext)` so saved photos appear in Gallery. |
| **iOS** | Add usage descriptions in `Info.plist`: `NSCameraUsageDescription`, `NSPhotoLibraryAddUsageDescription`, `NSMicrophoneUsageDescription` (for video). |
| **Desktop** | No extra setup. |
| **WASM** | Camera not supported; `rememberCameraKState` returns an error state. |

### Packages

Camera: **`core.domain.camera.*`** (compose, state, controller, result, enums, permissions, video).  
Image save & EXIF: **`core.util.image`** (`PhotoSaveUtils`, `SavePhotoResult`).

---

## Permissions

Check and request runtime permissions (Android/iOS):

```kotlin
val factory = rememberPermissionsControllerFactory()
val permissionsController = factory.createPermissionsController()

// Check state
val state = permissionsController.getPermissionState(Permission.CAMERA)
if (state != PermissionState.Granted) {
    try {
        permissionsController.providePermission(Permission.CAMERA)
    } catch (e: DeniedAlwaysException) {
        permissionsController.openAppSettings()
    } catch (e: RequestCanceledException) {
        // User canceled
    }
}

val hasLocation = permissionsController.isPermissionGranted(Permission.LOCATION)
```

---

## Push Notifications

`core.push` covers the push notification flow every app rebuilds: notification permission status, the app's own request and how often it may be shown, the system prompt, and registering the device token with your backend (once per token, again after a rotation, removed on sign-out). Firebase Cloud Messaging delivers the pushes; your app keeps its own channel, strings, backend calls and tap routing.

Create one `PushNotifications` per app (for example a DI singleton):

```kotlin
val pushNotifications = PushNotifications(
    options = PushNotificationOptions(
        androidChannelId = "app_notifications",
        androidChannelName = "Notifications",
        softAskPolicy = SoftAskPolicy(maxAskCount = 7, minimumInterval = 3.days),
        // false on iOS: App Store Guideline 5.1.1(iv), no own message in front of the system prompt
        isAppMessageShownBeforeSystemPrompt = !isIos,
    ),
    pushTokenSource = PlatformPushTokenSource,
    pushTokenRegistrar = { token, platform, deviceIdentifier -> backend.registerDevice(token, platform, deviceIdentifier) },
    pushTokenUnregistrar = { token, deviceIdentifier -> backend.unregisterDevice(token, deviceIdentifier) },
    platformConfigProvider = multiPlatformUtils::getPlatformConfig,
    notificationPermissionStorage = NotificationPermissionStorageImpl(settings),
    isSignedIn = { authStorage.isUserLoggedIn() },
)
```

Use it from screens with the screen's `PermissionsController` (see [Permissions](#permissions)):

| Situation | Call |
|-----------|------|
| App start or a screen opens: register if allowed | `registerTokenIfPermitted(permissionsController)` |
| May the screen show its own request now? (books the ask) | `softAsk(permissionsController)` → status or `null` |
| Skip the own request and open the system prompt directly | `isSystemPromptShownDirectly(status)` |
| User tapped "Turn on" | `enableNotifications(permissionsController)` → `NotificationPromptResult` (system prompt, or settings when blocked) |
| iOS: register rotated tokens | `registerTokenUpdates()` once from the app scope |
| Sign-out | `unregisterToken()` |

Failures are logged and never thrown (coroutine cancellation still is), so a declined notification never shows an error.

**Android**

1. Apply the Google services plugin with your `google-services.json` (Firebase Messaging comes with core).
2. Subclass the ready service and declare it with the permission in `AndroidManifest.xml`:

```kotlin
class AppMessagingService : PushNotificationMessagingService() {
    override val pushNotifications: PushNotifications by inject()
    override val launchActivityClass = MainActivity::class.java
    override val notificationIconResourceId = R.drawable.ic_notification

    // Optional: tap routing and the app's own data-only messages
    override fun resolveDeepLink(message: RemoteMessage): String? = message.data["deepLink"]
    override fun handleDataMessage(message: RemoteMessage): Boolean = false
}
```

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service android:name=".AppMessagingService" android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

The service creates the channel from the options, shows notification messages, opens `launchActivityClass` with the deep link as intent data, and registers rotated tokens.

**iOS**

Add FirebaseMessaging to the iOS app, enable the Push Notifications capability and upload the APNs key to Firebase. Then hand the APNs token to Firebase and the FCM token to core in `AppDelegate` (the class must be visible to Swift: export core from your framework, or call it through a function in your shared module):

```swift
func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    FirebaseApp.configure()
    UNUserNotificationCenter.current().delegate = self
    Messaging.messaging().delegate = self
    // On every launch: the APNs token changes across iOS updates, restores and reinstalls, and iOS only
    // hands out the current one in response to this call. Without it FCM accepts every send and APNs
    // drops it without an error.
    application.registerForRemoteNotifications()
    return true
}

func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    Messaging.messaging().apnsToken = deviceToken
}

func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
    guard let token = fcmToken, !token.isEmpty else { return }
    IosPushTokenBridge.shared.updateToken(token: token)
}
```

`PlatformPushTokenSource` reads that token on iOS, Firebase Messaging on Android, and reports none on desktop and web. The **Push Notifications Demo** screen in the sample app runs the whole flow against a fake backend.

---

## Auth Session

`core.session` covers the sign-in session every app wires by hand: storing the token pair, handing the access token to the HTTP client, renewing the tokens when the backend rejects the access token, and clearing everything on sign-out. A renewal that finishes after a sign-out, or after the next sign-in, is dropped instead of writing old tokens back. Your app keeps its renew and revoke endpoints and its own cleanup.

Create one `AuthSessionManager` per app (for example a DI singleton):

```kotlin
val authSessionManager = AuthSessionManager(
    authSessionOptions = AuthSessionOptions(
        renewTokens = { refreshToken -> authApi.renew(refreshToken).let { AuthTokens(it.accessToken, it.refreshToken) } },
        onAccessTokenChanged = { accessToken -> devengNetworkingModule.setToken(accessToken.orEmpty()) },
        revokeRefreshToken = { refreshToken -> authApi.revoke(refreshToken) }, // optional
    ),
    // Pass your current keys (AuthSessionStorageKeys(accessTokenKey = "TOKEN", ...)) to keep signed-in users signed in.
    authCredentialsStorage = AuthCredentialsStorageImpl(settings),
)

// deveng-networking-kmp: renew on HTTP 401 and start with the stored token
DevengNetworkingConfig(
    token = authSessionManager.getAccessToken().orEmpty(),
    sessionRefresher = DevengSessionRefresher(authSessionManager::refreshSession),
)
```

| Situation | Call |
|-----------|------|
| Sign-in or sign-up succeeded | `signIn(accessToken, refreshToken, onSignedIn = { syncBillingUser() })` |
| Sign-out | `signOut(beforeSignOut = { pushNotifications.unregisterToken() }, onCleared = { productStores.forEach { it.clear() } })` |
| Show the signed-in or signed-out screens | `isLoggedIn` (`StateFlow<Boolean>`) |
| Read claims of the access token | `decodeClaims<UserClaims>()` (null when signed out; signature not verified) |
| Renew before a long background job | `devengNetworkingModule.refreshSession()` (shares the client's single-flight lock; do not call `authSessionManager.refreshSession()` directly) |

`signIn` hands the token to the client before `isLoggedIn` turns true. `signOut` runs `beforeSignOut` while requests are still authenticated, then turns `isLoggedIn` false and removes the stored tokens, revokes the refresh token, drops the client's token and runs `onCleared`; a failing hook or revoke is logged and never stops the local sign-out, and the steps after `isLoggedIn` turns false still run when the calling screen's scope is cancelled. A revoke that does not answer within `revokeTimeout` (10 s by default) is abandoned. If the app process dies during the revoke, no token is left stored, but `onCleared` does not run. A renewal without a new refresh token keeps the stored one. The **Auth Session Demo** screen in the sample app shows the flow, including a sign-out while a renewal is running.

---

## Pagination

Load paged data with `PaginatedFlowLoader` and show in `PaginatedListView`:

```kotlin
val loader = remember(scope, pageSize) {
    PaginatedFlowLoader(
        initialKey = 0,
        scope = scope,
        pageSize = 20,
        pageSource = { page, size ->
            val response = api.fetchPage(page, size)
            PageResult(items = response.items, hasNextPage = response.hasNextPage)
        },
        getNextKey = { page, _ -> page + 1 }
    )
}
LaunchedEffect(Unit) { loader.reset() }

val state by loader.state.collectAsState()
PaginatedListView(
    state = state,
    onScrollReachNextPageThreshold = { loader.loadNextPage() },
    onSwipeAtListsEnd = { loader.reset() },
    itemSlot = { item -> ItemRow(item) }
)

// Refresh or new filters
loader.updatePageSource(newKey, newPageSource, reload = true)
```

---

## BaseViewModel and interceptors

`BaseViewModel<State, Event, Effect>` keeps a screen's `State` (UI state, loading flag, error), handles `Event`s in order and sends one-time `Effect`s. `StateLayout` draws the loading indicator and the error dialog from that state, and `InitEffect` runs a screen's first load once per ViewModel.

```kotlin
class ProfileViewModel(private val fetchProfile: FetchProfileUseCase) :
    BaseViewModel<ProfileUiState, ProfileEvent, ProfileEffect>(initialState = ProfileUiState()) {

    override fun handleEvents(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Load -> launch(dispatcher = Dispatchers.IO) {
                val profile = fetchProfile()
                setState { copy(name = profile.name) }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state = viewModel.getState()
    InitEffect(viewModel = viewModel) { isInit -> if (isInit) viewModel.setEvent(ProfileEvent.Load) }
    StateLayout(state = state, onCloseError = viewModel::hideError) {
        ProfileContent(state = state.uiState)
    }
}
```

`launch` shows the loading flag while the block runs and sends a failure to `setError`, which shows the error dialog. With `notifyError = false` the failure is not shown.

App rules such as logging or ending the session live in a `ViewModelInterceptor`, registered once at app start with `ViewModelInterceptors.register(...)`. Every ViewModel calls each interceptor:

- `onEvent` and `onEffect` for every event and effect.
- `onError` for a failure passed to `setError`. It returns an `ErrorDecision`: `SHOW` (show the dialog), `SILENT` (log only) or `HANDLED` (the interceptor took care of it). When interceptors answer differently, `HANDLED` wins over `SILENT`, and `SILENT` wins over `SHOW`.
- `onSilentError` when a `launch` with `notifyError = false` fails. The user is not shown this failure.

The sample app's "BaseViewModel Demo" screen shows each case.

---

## Client Logs (deveng-pinqloq)

The `deveng-pinqloq` module sends client logs to a log service. The app gives it an endpoint and options, and gets back a `PinqloqLogger`. The module builds the request, adds the app version and device values, cuts long events, and never throws from `log`.

```kotlin
implementation("global.deveng:pinqloq-kmp:VERSION")
```

The app registers `DevengNetworkingModule` and `MultiPlatformUtils` in Koin, then adds the module:

```kotlin
val clientLogModule = pinqloqClientModule(
    clientOptions = PinqloqClientOptions(
        endpoint = "client-logs",
        requestMapper = PinqloqRequestMappers.LevelWithDeviceIdentifier,
        enabled = { authSessionManager.isLoggedIn.value },
    ),
    metadataProvider = PinqloqMetadataProvider { mapOf("userId" to currentUserId()) },
)

val logger: PinqloqLogger by inject()
logger.log(PinqloqLevel.Warning, "Profile photo upload failed", detail = mapOf("status" to "503"))
```

### Options

| Option | Default | Meaning |
|--------|---------|---------|
| `endpoint` | none | Path that receives one log per request. Required. |
| `batchEndpoint` | `null` | Path that receives a batch. Required when `batchSize` is above 1. |
| `secretKey` | `null` | Sent as the `X-Secret-Key` header when set. |
| `collectionName` | `null` | Sent as `collectionName` in batch requests only. |
| `maxEventLength` | `1000` | A longer event is cut to this length and ends with `...(truncated)`. |
| `batchSize` | `1` | `1` sends every log at once. A larger value buffers logs and sends them together. |
| `flushInterval` | `2.seconds` | Longest time a buffered log waits before it is sent. |
| `queueCapacity` | `1000` | Most logs that can wait. When the queue is full, the oldest log is dropped. |
| `enabled` | `{ true }` | Checked on every log. When it returns `false`, the log is not built or sent. |
| `requestMapper` | `LevelWithDeviceIdentifier` | Wire shape of one log. |
| `failureListener` | `LogToConsole` | Called with a `PinqloqFailure` when a log is dropped. |

A batch request body is `{"collectionName": …, "logs": […]}`, and each item in `logs` is the shape the `requestMapper` writes for one log.

### Request shapes

- `PinqloqRequestMappers.LevelWithDeviceIdentifier`: `event`, `deviceIdentifier`, `level`, `appVersionName`, `metadata`, `detail`. `metadata` also carries `deviceName`.
- `PinqloqRequestMappers.LogLevelWithDate`: `event`, `logLevel`, `metadata`, `detail`, `date`. `metadata` also carries `appVersionName`, `deviceUid` and `deviceName`.

An empty `metadata` or `detail` is left out of the request. For another shape, implement `PinqloqRequestMapper`.

### Metadata keys

`metadata` is merged in this order, and a later source replaces an earlier one with the same key: the map passed to `log`, then the `PinqloqMetadataProvider` map, then the device values of the `requestMapper`. Every replaced key is written to `CustomLogger`.

### Failures

`log` does not throw. A failure goes to `failureListener` with a reason: `ENTRY_REJECTED` (the `enabled` check or the metadata provider threw), `DELIVERY_FAILED` (the request failed; a failed batch is dropped, not retried) or `QUEUE_OVERFLOW`. Only coroutine cancellation is rethrown.

The sample app's "Pinqloq Client Log Demo" screen shows an immediate logger, a batching logger, the `enabled` check and truncation. It records requests in memory instead of sending them.

---

## File picker (deveng-filepicker)

`deveng-filepicker` (artifact `global.deveng:filepicker-kmp`) picks photos, videos and documents on Android, iOS, desktop and web. It is its own module so apps that do not pick files do not pull in its dependencies. The package is `com.deveng.calf`.

```kotlin
val pickerLauncher = rememberFilePickerLauncher(
    type = FilePickerFileType.ImageVideo,
    selectionMode = FilePickerSelectionMode.Multiple(maxItems = 5),
    settings = FilePickerSettings(maxFileSizeBytes = 260L * 1024 * 1024),
    onResult = { files -> /* List<KmpFile> */ },
)

CustomButton(text = "Pick", onClick = pickerLauncher::launch)
```

- `FilePickerFileType` chooses what can be picked (`Image`, `Video`, `ImageVideo`, `Pdf`, `Custom`, `Extension`, `All`, …).
- `FilePickerSelectionMode.Single` or `Multiple(maxItems)`.
- `FilePickerSettings.maxFileSizeBytes` applies on iOS only: a picked asset over the limit is returned without a local copy, with its size, so the app can reject it. On the other platforms the app checks `KmpFile.getSize()` itself.
- `KmpFile` gives the name, size and bytes of a picked file (`getName()`, `getSize()`, `readByteArray()`).
- `rememberFileSaverLauncher` saves a file; on the web it starts a browser download.
- The fork has no camera source. Taking a photo stays with the core camera APIs (see [Camera](#camera)).
- On desktop the picker loads a native library from the app's resources (`native/<os>-<arch>/`); the module does not ship it.

Sample: the **Open File Picker Demo** screen in `sample/composeApp`.

---

## Web analytics (Google Analytics 4)

On Android and iOS the conversion analytics (`ConversionEventTracker`, `ConversionAnalyticsClient`) reach Firebase and Meta. On the web (wasmJs) `GoogleTagConversionAnalyticsClient` (`core.analytics.data.client.googletag`) is the Google Analytics 4 sink. It loads the Google tag script itself, so a page needs no script tag.

```kotlin
// wasmJsMain of the product
val webAnalytics = GoogleTagConversionAnalyticsClient(
    GoogleTagOptions(
        measurementId = "G-XXXXXXXXXX",
        enabledHostnames = setOf("app.example.com"),
        consent = ConsentOptions(storageKey = "app.analytics.consent", privacyUrl = "privacy.html#storage"),
        screenView = ScreenViewOptions(pathPrefix = "/app/", titlePrefix = "App · "),
        eventName = { event -> event.firebaseName }
    )
)
// Koin: single<ConversionAnalyticsClient> { webAnalytics }
webAnalytics.trackScreenView("Home") // from the product's navigation listener
```

- **Hostnames.** The client does nothing, and loads no script, on a page whose hostname is not in `enabledHostnames`. The default is no hostname, so a development server never sends data.
- **Consent.** Analytics storage stays denied until the visitor grants it, and `ConsentOptions` is what lets the visitor grant it. The banner texts are in English by default; pass `ConsentTexts` for another language. The banner uses the CSS classes `<cssClassPrefix>`, `__text`, `__actions`, `__button` and `__button--accept` (prefix `analytics-consent` by default), which the product styles. A declined choice removes the `_ga` cookies. With `showBanner = false` the product shows its own banner and calls `window.<globalObjectName>.grantConsent()` or `denyConsent()`; `showConsentBanner()` opens the banner again from a "cookie settings" link.
- **Event names.** `eventName` picks which name of the `AnalyticsEvent` Google Analytics receives. A name that is not 1-40 characters of lower case letters, digits and underscores starting with a letter is dropped. The parameters in `numericParameterNames` (`value` and `tax` by default) are sent as numbers when their value is a number.
- **Global object.** The client adds `window.devengGoogleTag`; change the name with `globalObjectName`.
- **Desktop** keeps `NoOpConversionAnalyticsClient`.

The sample app's wasm entry point (`WebAnalyticsSample.kt`) shows the wiring and sends a `page_view` and one conversion event on `localhost`.

---

## Sample App

The **sample** app in `sample/composeApp` demonstrates theming, main UI components, and multiplatform run targets. Open the project in Android Studio and run the `composeApp` configuration for Android, iOS, Desktop, or Wasm.

---

## Building

```bash
git clone https://github.com/Deveng-Group/deveng-core-kmp.git
cd deveng-core-kmp
```

Open in Android Studio or build from the command line:

```bash
./gradlew :deveng-core:build
./gradlew :sample:composeApp:assembleDebug   # Android sample
```

---

## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0). See the [LICENSE](LICENSE) file for details.
