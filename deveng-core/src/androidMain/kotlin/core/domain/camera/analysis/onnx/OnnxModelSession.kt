package core.domain.camera.analysis.onnx

import ai.onnxruntime.NodeInfo
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Owns one file-backed ONNX Runtime session. */
internal class OnnxModelSession private constructor(
    val session: OrtSession,
    val metadata: OnnxModelMetadata
) : AutoCloseable {
    private val isClosed = AtomicBoolean(false)

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            session.close()
        }
    }

    companion object {
        /**
         * Opens a session from a model stored as a regular file on the Android device.
         *
         * The session captures generic tensor metadata. Model-specific profiles validate the
         * semantic layout and shape contract after this method returns; unsupported models fail
         * before any inference buffer is used.
         */
        fun open(
            modelFile: File,
            environment: OrtEnvironment? = null,
            intraOpNumThreads: Int? = null,
            interOpNumThreads: Int? = null,
            executionProvider: OnnxExecutionProvider = OnnxExecutionProvider.CPU,
            xnnpackIntraOpNumThreads: Int? = null,
            disableIntraOpSpinning: Boolean = false
        ): OnnxModelSession {
            val runtimeEnvironment = environment ?: OrtEnvironment.getEnvironment()

            val session = OrtSession.SessionOptions().use { options ->
                options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

                intraOpNumThreads?.let(options::setIntraOpNumThreads)
                interOpNumThreads?.let(options::setInterOpNumThreads)

                if (disableIntraOpSpinning) {
                    options.addConfigEntry("session.intra_op.allow_spinning", "0")
                }

                if (executionProvider == OnnxExecutionProvider.XNNPACK) {
                    options.addXnnpack(
                        xnnpackIntraOpNumThreads
                            ?.let { mapOf("intra_op_num_threads" to it.toString()) }
                            .orEmpty()
                    )
                }
                runtimeEnvironment.createSession(modelFile.absolutePath, options)
            }

            val modelMetadata = try {
                OnnxModelMetadata(
                    inputs = session.inputInfo.entries.map { (name, node) ->
                        val tensorInfo = node.info as TensorInfo
                        OnnxTensorMetadata(
                            name = name,
                            type = tensorInfo.type,
                            shape = tensorInfo.shape.toList()
                        )
                    },
                    outputs = session.outputInfo.entries.map { (name, node) ->
                        val tensorInfo = node.info as TensorInfo
                        OnnxTensorMetadata(
                            name = name,
                            type = tensorInfo.type,
                            shape = tensorInfo.shape.toList()
                        )
                    }
                )
            } catch (error: Throwable) {
                session.close()
                throw error
            }

            return OnnxModelSession(session, modelMetadata)
        }
    }
}
