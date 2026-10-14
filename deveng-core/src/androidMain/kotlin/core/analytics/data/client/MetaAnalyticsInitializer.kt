package core.analytics.data.client

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

/**
 * Initializes the Facebook SDK for Meta App Events. The consuming app must disable the SDK's
 * auto-init in its manifest so nothing runs until a real Facebook App ID is configured; with a blank
 * App ID this is a no-op and the SDK never touches the app. Call once from [Application.onCreate].
 */
object MetaAnalyticsInitializer {
    fun initialize(application: Application, facebookAppId: String, facebookClientToken: String) {
        if (facebookAppId.isBlank()) return

        FacebookSdk.setApplicationId(facebookAppId)
        FacebookSdk.setClientToken(facebookClientToken)
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.fullyInitialize()
        AppEventsLogger.activateApp(application)
    }
}
