package core.util

/**
 * Restarts the running app process so a platform-level locale override applied at the
 * next cold start (see [SelectedLanguagePreference]) takes effect. On platforms that
 * cannot self-relaunch (iOS), [restart] is a no-op and the UI must instruct the user to
 * close and reopen the app manually.
 */
interface AppRestarter {
    fun restart()
}
