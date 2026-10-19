package core.session.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Identifies the current sign-in. A token renewal can finish after the sign-out or sign-in that
 * replaced its session; it captures the generation when it starts and stores its tokens only when the
 * generation is unchanged. The lock spans the check and the write, so a sign-out cannot land between them.
 */
internal class SessionGeneration {
    private val generationMutex = Mutex()
    private var currentGeneration = 0L

    suspend fun <T> withCurrent(block: suspend (generation: Long) -> T): T = generationMutex.withLock {
        block(currentGeneration)
    }

    suspend fun <T> advance(block: suspend (newGeneration: Long) -> T): T = generationMutex.withLock {
        currentGeneration += 1
        block(currentGeneration)
    }

    suspend fun runIfUnchanged(expectedGeneration: Long, block: suspend () -> Unit): Boolean =
        generationMutex.withLock {
            if (currentGeneration != expectedGeneration) {
                return@withLock false
            }

            block()
            true
        }
}
