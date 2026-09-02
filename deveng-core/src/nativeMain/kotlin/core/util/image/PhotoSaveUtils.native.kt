package core.util.image

import core.domain.camera.utils.toNSData
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataCreateMutable
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreServices.kUTTypeJPEG
import platform.Foundation.NSArray
import platform.Foundation.NSDictionary
import platform.Foundation.NSFileManager
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.ImageIO.CGImageDestinationAddImageFromSource
import platform.ImageIO.CGImageDestinationAddImageAndMetadata
import platform.ImageIO.CGImageDestinationCreateWithData
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageMetadataCreateMutable
import platform.ImageIO.CGImageMetadataCreateMutableCopy
import platform.ImageIO.CGImageMetadataSetValueMatchingImageProperty
import platform.ImageIO.CGImageSourceCopyMetadataAtIndex
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object PhotoSaveUtils {
    private val gpsDictionaryKey: NSString = NSString.create(string = "{GPS}")
    private val gpsLatitudeKey: NSString = NSString.create(string = "Latitude")
    private val gpsLatitudeRefKey: NSString = NSString.create(string = "LatitudeRef")
    private val gpsLongitudeKey: NSString = NSString.create(string = "Longitude")
    private val gpsLongitudeRefKey: NSString = NSString.create(string = "LongitudeRef")
    private val exifDictionaryKey: NSString = NSString.create(string = "{Exif}")
    private val exifDateTimeOriginalKey: NSString = NSString.create(string = "DateTimeOriginal")
    private val exifDateTimeDigitizedKey: NSString = NSString.create(string = "DateTimeDigitized")
    private val exifOffsetTimeOriginalKey: NSString = NSString.create(string = "OffsetTimeOriginal")

    actual fun setApplicationContext(context: Any?) {}

    actual fun imageBytesWithNormalOrientation(imageBytes: ByteArray): ByteArray = imageBytes

    actual fun savePhoto(imageBytes: ByteArray, targetPath: String): SavePhotoResult = try {
        val nsData = imageBytes.toNSData()
        val parentPath = targetPath.substringBeforeLast("/", "")
        if (parentPath.isNotEmpty()) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                parentPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        val success = NSFileManager.defaultManager.createFileAtPath(targetPath, nsData, null)
        if (!success) {
            return SavePhotoResult.Error(Exception("createFileAtPath returned false"))
        }
        saveToPhotosLibrary(targetPath)
        SavePhotoResult.Success(targetPath)
    } catch (e: Exception) {
        SavePhotoResult.Error(e)
    }

    private fun saveToPhotosLibrary(filePath: String) {
        var saveError: String? = null
        val semaphore = platform.darwin.dispatch_semaphore_create(0)

        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            changeBlock = {
                PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(
                    NSURL.fileURLWithPath(filePath),
                )
            },
            completionHandler = { success, error ->
                if (!success || error != null) {
                    saveError = error?.localizedDescription ?: "Failed to save to Photos"
                    platform.Foundation.NSLog("PhotoSaveUtils: Failed to save to Photos: $saveError")
                }
                platform.darwin.dispatch_semaphore_signal(semaphore)
            },
        )

        platform.darwin.dispatch_semaphore_wait(semaphore, platform.darwin.DISPATCH_TIME_FOREVER)
    }

    actual fun addLocationExif(
        imageBytes: ByteArray,
        latitude: Double,
        longitude: Double,
    ): ByteArray {
        if (imageBytes.isEmpty()) {
            return imageBytes
        }
        val inData = imageBytes.toCFData() ?: return imageBytes
        val source = CGImageSourceCreateWithData(inData, options = null)
        CFRelease(inData)
        if (source == null) {
            return imageBytes
        }
        val outMutableData = CFDataCreateMutable(allocator = kCFAllocatorDefault, capacity = 0)
        if (outMutableData == null) {
            CFRelease(source)
            return imageBytes
        }
        val dest = CGImageDestinationCreateWithData(
            data = outMutableData,
            type = kUTTypeJPEG,
            count = 1u,
            options = null,
        )
        if (dest == null) {
            CFRelease(outMutableData)
            CFRelease(source)
            return imageBytes
        }
        try {
            val imageRef = CGImageSourceCreateImageAtIndex(source, 0u, options = null) ?: run {
                return imageBytes
            }
            val sourceMetadata = CGImageSourceCopyMetadataAtIndex(source, 0u, options = null)
            val mutableMetadata = if (sourceMetadata != null) {
                CGImageMetadataCreateMutableCopy(sourceMetadata)
            } else {
                CGImageMetadataCreateMutable()
            } ?: run {
                CFRelease(imageRef)
                sourceMetadata?.let { CFRelease(it) }
                return imageBytes
            }
            try {
                val gpsDictionaryName = CFStringCreateWithCString(
                    alloc = kCFAllocatorDefault,
                    cStr = "{GPS}",
                    encoding = kCFStringEncodingUTF8,
                )
                val gpsLatitudeName = CFStringCreateWithCString(
                    alloc = kCFAllocatorDefault,
                    cStr = "Latitude",
                    encoding = kCFStringEncodingUTF8,
                )
                val gpsLatitudeRefName = CFStringCreateWithCString(
                    alloc = kCFAllocatorDefault,
                    cStr = "LatitudeRef",
                    encoding = kCFStringEncodingUTF8,
                )
                val gpsLongitudeName = CFStringCreateWithCString(
                    alloc = kCFAllocatorDefault,
                    cStr = "Longitude",
                    encoding = kCFStringEncodingUTF8,
                )
                val gpsLongitudeRefName = CFStringCreateWithCString(
                    alloc = kCFAllocatorDefault,
                    cStr = "LongitudeRef",
                    encoding = kCFStringEncodingUTF8,
                )
                val northSouthValue = CFStringCreateWithCString(
                    alloc = kCFAllocatorDefault,
                    cStr = if (latitude >= 0.0) "N" else "S",
                    encoding = kCFStringEncodingUTF8,
                )
                val eastWestValue = CFStringCreateWithCString(
                    alloc = kCFAllocatorDefault,
                    cStr = if (longitude >= 0.0) "E" else "W",
                    encoding = kCFStringEncodingUTF8,
                )
                if (
                    gpsDictionaryName == null ||
                    gpsLatitudeName == null ||
                    gpsLatitudeRefName == null ||
                    gpsLongitudeName == null ||
                    gpsLongitudeRefName == null ||
                    northSouthValue == null ||
                    eastWestValue == null
                ) {
                    return imageBytes
                }
                CGImageMetadataSetValueMatchingImageProperty(
                    mutableMetadata,
                    gpsDictionaryName,
                    gpsLatitudeRefName,
                    northSouthValue,
                )
                CGImageMetadataSetValueMatchingImageProperty(
                    mutableMetadata,
                    gpsDictionaryName,
                    gpsLatitudeName,
                    NSNumber(double = kotlin.math.abs(latitude)) as platform.CoreFoundation.CFTypeRef?,
                )
                CGImageMetadataSetValueMatchingImageProperty(
                    mutableMetadata,
                    gpsDictionaryName,
                    gpsLongitudeRefName,
                    eastWestValue,
                )
                CGImageMetadataSetValueMatchingImageProperty(
                    mutableMetadata,
                    gpsDictionaryName,
                    gpsLongitudeName,
                    NSNumber(double = kotlin.math.abs(longitude)) as platform.CoreFoundation.CFTypeRef?,
                )
                CGImageDestinationAddImageAndMetadata(
                    idst = dest,
                    image = imageRef,
                    metadata = mutableMetadata,
                    options = null,
                )
                CFRelease(gpsDictionaryName)
                CFRelease(gpsLatitudeName)
                CFRelease(gpsLatitudeRefName)
                CFRelease(gpsLongitudeName)
                CFRelease(gpsLongitudeRefName)
                CFRelease(northSouthValue)
                CFRelease(eastWestValue)
            } finally {
                CFRelease(imageRef)
                CFRelease(mutableMetadata)
                sourceMetadata?.let { CFRelease(it) }
            }
            if (!CGImageDestinationFinalize(idst = dest)) {
                return imageBytes
            }
            val result = cfDataToByteArray(outMutableData) ?: run {
                return imageBytes
            }
            return result
        } catch (_: Exception) {
            return imageBytes
        } finally {
            CFRelease(dest)
            CFRelease(outMutableData)
            CFRelease(source)
        }
    }

    actual fun readLocationFromExif(imageBytes: ByteArray): Pair<Double, Double>? {
        if (imageBytes.isEmpty()) {
            return null
        }
        val inData = imageBytes.toCFData() ?: return null
        val source = CGImageSourceCreateWithData(inData, options = null)
        CFRelease(inData)
        if (source == null) {
            return null
        }
        return try {
            val props = CGImageSourceCopyPropertiesAtIndex(isrc = source, index = 0u, options = null)
                ?: return null
            try {
                val dict = (props as? NSDictionary) ?: return null
                val gps = dict.objectForKey(gpsDictionaryKey) as? NSDictionary
                    ?: return null
                readGpsLatLon(gps)
            } finally {
                CFRelease(props)
            }
        } finally {
            CFRelease(source)
        }
    }

    actual fun readCaptureDateTimeFromExif(imageBytes: ByteArray): ExifCaptureDateTime? {
        if (imageBytes.isEmpty()) {
            return null
        }
        val inData = imageBytes.toCFData() ?: return null
        val source = CGImageSourceCreateWithData(inData, options = null)
        CFRelease(inData)
        if (source == null) {
            return null
        }
        return try {
            val props = CGImageSourceCopyPropertiesAtIndex(isrc = source, index = 0u, options = null)
                ?: return null
            try {
                val dict = (props as? NSDictionary) ?: return null
                val exif = dict.objectForKey(exifDictionaryKey) as? NSDictionary
                    ?: return null
                val dateTime = (exif.objectForKey(exifDateTimeOriginalKey) as? NSString)?.description
                    ?: (exif.objectForKey(exifDateTimeDigitizedKey) as? NSString)?.description
                    ?: return null
                val offset = (exif.objectForKey(exifOffsetTimeOriginalKey) as? NSString)?.description
                parseExifDateTime(raw = dateTime, offsetRaw = offset)
            } finally {
                CFRelease(props)
            }
        } finally {
            CFRelease(source)
        }
    }

    private fun ByteArray.toCFData(): platform.CoreFoundation.CFDataRef? = usePinned { pinned ->
        CFDataCreate(
            allocator = kCFAllocatorDefault,
            bytes = pinned.addressOf(0).reinterpret<UByteVar>(),
            length = size.convert(),
        )
    }

    private fun cfDataToByteArray(data: platform.CoreFoundation.CFDataRef?): ByteArray? {
        if (data == null) {
            return null
        }
        val len = CFDataGetLength(data).toInt()
        if (len <= 0) {
            return null
        }
        val ptr = CFDataGetBytePtr(data) ?: return null
        return ByteArray(len).also { out ->
            out.usePinned { pinned ->
                memcpy(pinned.addressOf(0), ptr, len.convert())
            }
        }
    }

    private fun nsRefString(s: String): NSString =
        NSString.create(string = s)

    private fun readGpsLatLon(gps: NSDictionary): Pair<Double, Double>? {
        val lat = readSignedGpsComponent(
            gps = gps,
            valueKey = gpsLatitudeKey,
            refKey = gpsLatitudeRefKey,
            positiveRef = "N",
            negativeRef = "S",
        ) ?: return null
        val lon = readSignedGpsComponent(
            gps = gps,
            valueKey = gpsLongitudeKey,
            refKey = gpsLongitudeRefKey,
            positiveRef = "E",
            negativeRef = "W",
        ) ?: return null
        return lat to lon
    }

    private fun readSignedGpsComponent(
        gps: NSDictionary,
        valueKey: NSString,
        refKey: NSString,
        positiveRef: String,
        negativeRef: String,
    ): Double? {
        val refObj = gps.objectForKey(refKey) ?: return null
        val ref = (refObj as? NSString)?.description ?: return null
        val value = gps.objectForKey(valueKey) ?: return null
        val magnitude = gpsValueToDecimalDegrees(value) ?: return null
        val signed = when (ref) {
            positiveRef -> kotlin.math.abs(magnitude)
            negativeRef -> -kotlin.math.abs(magnitude)
            else -> magnitude
        }
        return signed
    }

    private fun gpsValueToDecimalDegrees(value: Any?): Double? = when (value) {
        is NSNumber -> value.doubleValue
        is NSArray -> dmsArrayToDecimal(value)
        is NSString -> {
            val desc = value.description ?: return null
            rationalStringToDouble(desc)
        }
        else -> null
    }

    private fun dmsArrayToDecimal(arr: NSArray): Double? {
        val count = arr.count.toInt()
        if (count != 3) {
            return null
        }
        val d0 = gpsValueToDecimalDegrees(arr.objectAtIndex(0u)) ?: return null
        val d1 = gpsValueToDecimalDegrees(arr.objectAtIndex(1u)) ?: return null
        val d2 = gpsValueToDecimalDegrees(arr.objectAtIndex(2u)) ?: return null
        return d0 + d1 / 60.0 + d2 / 3600.0
    }

    private fun rationalStringToDouble(s: String): Double? {
        val parts = s.split("/")
        if (parts.size != 2) {
            return s.toDoubleOrNull()
        }
        val num = parts[0].toDoubleOrNull() ?: return null
        val den = parts[1].toDoubleOrNull() ?: return null
        if (den == 0.0) {
            return null
        }
        return num / den
    }
}
