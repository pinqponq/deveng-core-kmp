package core.data.upload

import co.touchlab.kermit.Logger
import core.data.temp.TempStorageDirProvider
import core.data.upload.session.BackgroundUploadFileNaming
import core.data.upload.session.BackgroundUploadRequestHandler
import core.data.upload.session.BackgroundUploadSessionScheduler
import core.domain.upload.BackgroundUploadSessionRenewer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

/**
 * Creates a [BackgroundUploadSessionScheduler] on its own background `NSURLSession` and registers it
 * in [BackgroundUploadSessionRegistry] under [sessionIdentifier]. Keep [sessionIdentifier] and
 * [bodyFileNamePrefix] stable across releases: iOS delivers results of transfers started by an
 * earlier install to the session with the same identifier.
 */
fun <T> createUrlSessionBackgroundUploadScheduler(
    sessionIdentifier: String,
    descriptorStorage: BackgroundUploadDescriptorStorage,
    requestHandler: BackgroundUploadRequestHandler<T>,
    sessionRenewer: BackgroundUploadSessionRenewer,
    bodyDirectoryProvider: TempStorageDirProvider,
    bodyFileNamePrefix: String,
    logTag: String,
    delegateLogTag: String,
): BackgroundUploadSessionScheduler<T> {
    // On Kotlin/Native an uncaught exception in a coroutine launched on Dispatchers.IO aborts the
    // whole process (SIGABRT); a background upload completion must never crash the app.
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Logger.e(throwable) { "[$logTag] Uncaught background coroutine exception" }
    }
    val scheduler = BackgroundUploadSessionScheduler(
        descriptorStorage = descriptorStorage,
        requestHandler = requestHandler,
        sessionRenewer = sessionRenewer,
        transport = UrlSessionBackgroundUploadTransport(sessionIdentifier = sessionIdentifier, delegateLogTag = delegateLogTag),
        fileStore = FileManagerBackgroundUploadFileStore(
            directoryProvider = bodyDirectoryProvider,
            fileNaming = BackgroundUploadFileNaming(fileNamePrefix = bodyFileNamePrefix),
        ),
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler),
        logTag = logTag,
    )
    BackgroundUploadSessionRegistry.register(sessionIdentifier = sessionIdentifier, scheduler = scheduler)
    return scheduler
}
