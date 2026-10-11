package core.util

import android.content.Context
import android.content.Intent

class AppRestarterImpl(
    private val context: Context
) : AppRestarter {
    override fun restart() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(launchIntent)
        Runtime.getRuntime().exit(0)
    }
}
