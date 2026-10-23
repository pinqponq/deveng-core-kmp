package core.pinqloq.fake

import core.pinqloq.domain.model.PinqloqLogEntry
import core.pinqloq.domain.repository.PinqloqLogRepository
import kotlinx.coroutines.CompletableDeferred

class FakePinqloqLogRepository : PinqloqLogRepository {
    val savedEntries = mutableListOf<PinqloqLogEntry>()
    val savedBatches = mutableListOf<List<PinqloqLogEntry>>()
    var failure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun saveLog(entry: PinqloqLogEntry) {
        failure?.let { throw it }
        savedEntries += entry
    }

    override suspend fun saveLogs(entries: List<PinqloqLogEntry>) {
        gate?.await()
        failure?.let { throw it }
        savedBatches += entries
    }
}
