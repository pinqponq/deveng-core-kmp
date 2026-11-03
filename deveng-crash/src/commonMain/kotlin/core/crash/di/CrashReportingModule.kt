package core.crash.di

import core.crash.data.createCrashReporter
import core.crash.domain.CrashReporter
import core.crash.domain.model.CrashReportingOptions
import core.util.multiplatform.MultiPlatformUtils
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module that provides [CrashReporter]. The app registers `MultiPlatformUtils`.
 */
fun crashReportingModule(crashReportingOptions: CrashReportingOptions): Module = module {
    single<CrashReporter> {
        val multiPlatformUtils = get<MultiPlatformUtils>()
        createCrashReporter(
            crashReportingOptions = crashReportingOptions,
            installIdProvider = { multiPlatformUtils.getPlatformConfig().uuid },
        )
    }
}
