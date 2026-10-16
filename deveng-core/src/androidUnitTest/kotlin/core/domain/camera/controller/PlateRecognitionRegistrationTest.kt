package core.domain.camera.controller

import core.domain.camera.analysis.plate.PlateRecognitionConfig
import core.domain.camera.analysis.plate.PlateRecognitionResult
import core.domain.camera.analysis.plate.manager.ModelSessionReleasePolicy
import core.domain.camera.analysis.plate.manager.PlateRecognitionModelManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PlateRecognitionRegistrationTest {
    @Test
    fun closeUnregistersOnlyOnce() {
        var unregisterCount = 0
        val handle = ManagedPlateRecognitionHandle { unregisterCount++ }

        handle.close()
        handle.close()

        assertEquals(1, unregisterCount)
    }

    @Test
    fun closeAndAwaitWaitsForTheAnalyzerCloseSignal() = runBlocking {
        var unregisterCount = 0
        lateinit var handle: ManagedPlateRecognitionHandle
        handle = ManagedPlateRecognitionHandle {
            unregisterCount++
            handle.markClosed()
        }

        handle.closeAndAwait()

        assertEquals(1, unregisterCount)
    }

    @Test
    fun automaticReleaseDeliversResultBeforeUnregistering() {
        val events = mutableListOf<String>()
        val handle = ManagedPlateRecognitionHandle { events += "unregister" }

        handle.deliver(
            result = PlateRecognitionResult(plate = "ABC123", confidence = 0.95f),
            onPlateRecognized = { events += "result:${it.plate}" },
            releaseAfterDelivery = true,
        )

        assertEquals(listOf("result:ABC123", "unregister"), events)
    }

    @Test
    fun manualPolicyLeavesRegistrationOpenAfterResult() {
        var unregisterCount = 0
        val handle = ManagedPlateRecognitionHandle { unregisterCount++ }

        handle.deliver(
            result = PlateRecognitionResult(plate = "ABC123", confidence = 0.95f),
            onPlateRecognized = {},
            releaseAfterDelivery = false,
        )

        assertEquals(0, unregisterCount)
        handle.close()
        assertEquals(1, unregisterCount)
    }

    @Test
    fun managerBackedRegistrationRequiresReadyModels() {
        val manager = PlateRecognitionModelManager()
        try {
            val error = assertFailsWith<IllegalStateException> {
                createManagedPlateRecognitionAnalyzer(manager)
            }

            assertTrue(error.message.orEmpty().contains("not ready"))
        } finally {
            manager.close()
        }
    }

    @Test
    fun managerBackedRegistrationRejectsAfterSuccessPolicy() {
        val manager = PlateRecognitionModelManager()
        try {
            val error = assertFailsWith<IllegalArgumentException> {
                createManagedPlateRecognitionAnalyzer(
                    modelManager = manager,
                    config = PlateRecognitionConfig(
                        modelSessionReleasePolicy = ModelSessionReleasePolicy.AFTER_SUCCESS,
                    ),
                )
            }

            assertTrue(error.message.orEmpty().contains("MANUAL"))
        } finally {
            manager.close()
        }
    }
}
