package core.util

import kotlin.system.exitProcess

class AppRestarterImpl : AppRestarter {
    override fun restart() {
        exitProcess(0)
    }
}
