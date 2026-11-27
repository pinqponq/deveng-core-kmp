package core.util

// iOS apps cannot programmatically relaunch themselves; the language-change confirmation
// dialog instructs the user to close and reopen the app manually instead.
class AppRestarterImpl : AppRestarter {
    override fun restart() = Unit
}
