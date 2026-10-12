package core.widget.android

import java.net.HttpURLConnection
import java.net.URL

private val SUCCESS_STATUS_CODES = 200..299

internal object HomeScreenWidgetImageDownloader {

    fun downloadImage(imageUrl: String, connectTimeoutMilliseconds: Int, readTimeoutMilliseconds: Int): ByteArray {
        val connection = URL(imageUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeoutMilliseconds
        connection.readTimeout = readTimeoutMilliseconds
        try {
            val statusCode = connection.responseCode
            if (statusCode !in SUCCESS_STATUS_CODES) throw IllegalStateException("Widget image download failed with status $statusCode.")
            return connection.inputStream.use { imageStream -> imageStream.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
