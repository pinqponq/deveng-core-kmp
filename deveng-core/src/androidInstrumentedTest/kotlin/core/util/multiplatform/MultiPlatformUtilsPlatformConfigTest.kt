package core.util.multiplatform

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultiPlatformUtilsPlatformConfigTest {

    @Test
    fun getPlatformConfig_onDevice_returnsReleaseVersion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val platformConfig = MultiPlatformUtils(context).getPlatformConfig()

        assertEquals(Build.VERSION.RELEASE, platformConfig.operatingSystemVersion)
    }
}
