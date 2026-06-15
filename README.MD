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
- [Pagination](#pagination)
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

### Components (overview)

- **Actions:** `CustomButton`, `CustomIconButton`, `LabeledSwitch`
- **Dialogs:** `CustomAlertDialog`, `CustomDialog`, `CustomDialogHeader`, `CustomDialogBody`
- **Inputs:** `CustomTextField`, `SearchField`, `PickerField`, `CustomDatePicker`, `CustomDateRangePicker`, `OtpView`
- **Layout:** `CustomHeader`, `RoundedSurface`, `LabeledSlot`, `Slot`, `ChipItem`
- **Lists & selection:** `OptionItemList`, `OptionItemLazyListDialog`, `OptionItemMultiSelectLazyListDialog`, `CustomDropDownMenu`
- **Navigation:** `NavigationMenu` (horizontal, expanded, collapsed)
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

### Best Practices (UI)

1. **Define theme once** — Create `ComponentTheme` at app level.
2. **Use theme for consistency** — Override only when needed.
3. **Font hierarchy** — Titles: 20–24sp (Bold), Body: 14–16sp (Regular), Buttons: 16–18sp (SemiBold).
4. **Accessibility** — Ensure sufficient color contrast and readable font sizes.
5. **Custom fonts** — Include all weights (400, 500, 600, 700) your app uses.

---

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
