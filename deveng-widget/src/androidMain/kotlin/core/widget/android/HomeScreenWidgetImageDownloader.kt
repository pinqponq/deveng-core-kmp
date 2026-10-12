package core.widget.android

import java.net.HttpURLConnection
import java.net.URL

private val SUCCESS_STATUS_CODES = 200..299

object HomeScreenWidgetImageDownloader {

    fun downloadImage(imageUrl: String, connectTimeoutMillis: Int, readTimeoutMillis: Int): ByteArray {
        val connection = URL(imageUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis
        try {
            val statusCode = connection.responseCode
            if (statusCode !in SUCCESS_STATUS_CODES) throw IllegalStateException("Widget image download failed with status $statusCode.")
            return connection.inputStream.use { imageStream -> imageStream.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
