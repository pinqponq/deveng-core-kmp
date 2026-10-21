package core.util

/**
 * Restarts the running app process so a platform-level locale override applied at the
 * next cold start (see [SelectedLanguagePreference]) takes effect. Only Android relaunches
 * the app. Desktop exits without relaunching. On iOS and the web [restart] does nothing, so
 * the UI must ask the user to close and reopen the app.
 */
interface AppRestarter {
    fun restart()
}
