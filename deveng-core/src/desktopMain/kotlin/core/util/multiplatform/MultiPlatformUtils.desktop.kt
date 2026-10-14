package core.util.multiplatform

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class MultiPlatformUtils {
    actual fun dialPhoneNumber(phoneNumber: String): Boolean {
        copyToClipBoard(phoneNumber)
        return false
    }

    actual fun copyToClipBoard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
    }

    actual fun openMapsWithLocation(latitude: Double, longitude: Double) {
        val url = "https://www.google.com/maps?q=$latitude,$longitude"
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        }
    }

    actual fun openUrl(url: String) {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        }
    }

    actual fun getPlatformConfig(): PlatformConfig {
        return PlatformConfig(
            platform = Platform.DESKTOP,
            systemLanguage = System.getProperty("user.language"),
            uuid = getDeviceId() ?: "unknown_desktop_uid",
            deviceName = getDeviceName(),
            packageVersionName = "0.0.0",
        )
    }

    actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // Location not supported on desktop
        return null
    }

    actual fun shareText(text: String) {
        if (text.isBlank()) return
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    private fun getDeviceId(): String? {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> WindowsUuid.fetch()
            os.contains("mac") -> MacUuid.fetch()
            os.contains("nux") -> File("/etc/machine-id").readText().trim()
            else -> null
        }
    }

    private fun getDeviceName(): String {
        return try {
            // Try to get computer name first
            System.getenv("COMPUTERNAME") // Windows
                ?: System.getenv("HOSTNAME") // Unix/Linux/macOS
                ?: System.getProperty("user.name") // Fallback to username
                ?: "Unknown Desktop"
        } catch (e: Exception) {
            "Unknown Desktop"
        }
    }
}

object WindowsUuid {
    fun fetch(): String? = try {
        // Query the registry for MachineGuid
        val cmd = arrayOf(
            "reg", "query",
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography",
            "/v", "MachineGuid"
        )
        Runtime.getRuntime().exec(cmd).inputStream.bufferedReader().useLines { lines ->
            lines
                .first { it.contains("MachineGuid", ignoreCase = true) }
                .trim()
                .split("\\s+".toRegex())
                .last()
        }
    } catch (e: Exception) {
        null
    }
}

object MacUuid {
    fun fetch(): String? = try {
        // ioreg + awk to pull IOPlatformUUID
        val cmd = arrayOf(
            "sh", "-c",
            "ioreg -rd1 -c IOPlatformExpertDevice | awk '/IOPlatformUUID/ { print \$3 }'"
        )
        Runtime.getRuntime().exec(cmd).inputStream.bufferedReader()
            .use { it.readText().trim().replace("\"", "") }
    } catch (e: Exception) {
        null
    }
}