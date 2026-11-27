package core.pinqloq.domain.repository

import core.pinqloq.domain.model.PinqloqLogEntry

interface PinqloqLogRepository {
    suspend fun saveLog(entry: PinqloqLogEntry)

    suspend fun saveLogs(entries: List<PinqloqLogEntry>)
}
