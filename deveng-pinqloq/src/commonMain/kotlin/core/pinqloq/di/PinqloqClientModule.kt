package core.pinqloq.di

import core.pinqloq.data.DevengPinqloqRequestSender
import core.pinqloq.data.MultiPlatformPinqloqPlatformInfoProvider
import core.pinqloq.data.PinqloqLogRepositoryImpl
import core.pinqloq.data.PinqloqRequestSender
import core.pinqloq.domain.PinqloqLogger
import core.pinqloq.domain.PinqloqLoggerImpl
import core.pinqloq.domain.PinqloqMetadataProvider
import core.pinqloq.domain.PinqloqPlatformInfoProvider
import core.pinqloq.domain.model.PinqloqClientOptions
import core.pinqloq.domain.repository.PinqloqLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module that provides [PinqloqLogger]. The app registers `DevengNetworkingModule` and `MultiPlatformUtils`.
 */
fun pinqloqClientModule(
    clientOptions: PinqloqClientOptions,
    metadataProvider: PinqloqMetadataProvider = PinqloqMetadataProvider.None
): Module = module {
    single<PinqloqRequestSender> { DevengPinqloqRequestSender(networkingModule = get()) }
    single<PinqloqPlatformInfoProvider> { MultiPlatformPinqloqPlatformInfoProvider(multiPlatformUtils = get()) }
    single<PinqloqLogRepository> {
        PinqloqLogRepositoryImpl(clientOptions = clientOptions, requestSender = get())
    }
    single<PinqloqLogger> {
        PinqloqLoggerImpl(
            clientOptions = clientOptions,
            logRepository = get(),
            platformInfoProvider = get(),
            metadataProvider = metadataProvider,
            flushScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }
}
