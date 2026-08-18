package core.domain.camera.analysis.onnx

import ai.onnxruntime.NodeInfo
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal enum class OnnxExecutionProvider {
    CPU,
    XNNPACK,
}

/** Owns one file-backed ONNX Runtime session. */
internal class OnnxModelSession private constructor(
    private val ortSession: OrtSession,
) : AutoCloseable {
    private val closeGuard = CloseOnce(ortSession::close)

    /** Returns the live native session for model-specific inference wrappers. */
    val session: OrtSession
        get() {
            if (closeGuard.isClosed) {
                throw OnnxModelException(
                    code = OnnxModelErrorCode.SESSION_CLOSED,
                    message = "ONNX Runtime session is already closed.",
                )
            }
            return ortSession
        }

    override fun close() {
        closeGuard.close()
    }

    companion object {
        /**
         * Opens a session from a model stored as a regular file on the Android device.
         *
         * Model-specific node names, element types, and shapes are validated by the detector or
         * OCR integration that owns this session. This layer verifies only the generic ONNX tensor
         * structure; model-specific incompatibilities remain the owning integration's
         * responsibility.
         */
        fun open(
            modelFile: File,
            environment: OrtEnvironment? = null,
            intraOpNumThreads: Int? = null,
            interOpNumThreads: Int? = null,
            executionProvider: OnnxExecutionProvider = OnnxExecutionProvider.CPU,
            xnnpackIntraOpNumThreads: Int? = null,
            disableIntraOpSpinning: Boolean = false,
        ): OnnxModelSession {
            require(intraOpNumThreads == null || intraOpNumThreads > 0) {
                "intraOpNumThreads must be positive when provided."
            }
            require(interOpNumThreads == null || interOpNumThreads > 0) {
                "interOpNumThreads must be positive when provided."
            }
            require(xnnpackIntraOpNumThreads == null || xnnpackIntraOpNumThreads > 0) {
                "xnnpackIntraOpNumThreads must be positive when provided."
            }
            require(
                executionProvider == OnnxExecutionProvider.XNNPACK ||
                    xnnpackIntraOpNumThreads == null,
            ) {
                "xnnpackIntraOpNumThreads requires the XNNPACK execution provider."
            }
            val validatedFile = validateModelFile(modelFile)
            val session = try {
                val runtimeEnvironment = environment ?: OrtEnvironment.getEnvironment()
                OrtSession.SessionOptions().use { options ->
                    intraOpNumThreads?.let(options::setIntraOpNumThreads)
                    interOpNumThreads?.let(options::setInterOpNumThreads)
                    if (disableIntraOpSpinning) {
                        options.addConfigEntry(ALLOW_INTRA_OP_SPINNING_KEY, "0")
                    }
                    if (executionProvider == OnnxExecutionProvider.XNNPACK) {
                        options.addXnnpack(
                            xnnpackIntraOpNumThreads
                                ?.let { mapOf(XNNPACK_INTRA_OP_THREADS_KEY to it.toString()) }
                                .orEmpty(),
                        )
                    }
                    runtimeEnvironment.createSession(validatedFile.absolutePath, options)
                }
            } catch (error: Exception) {
                throw modelInitializationFailure(validatedFile, error)
            } catch (error: LinkageError) {
                throw modelInitializationFailure(validatedFile, error)
            }

            try {
                validateTensorNodes(
                    nodes = session.inputInfo,
                    errorCode = OnnxModelErrorCode.INVALID_MODEL_INPUT,
                    nodeKind = "input",
                )
                validateTensorNodes(
                    nodes = session.outputInfo,
                    errorCode = OnnxModelErrorCode.INVALID_MODEL_OUTPUT,
                    nodeKind = "output",
                )
            } catch (error: Exception) {
                session.close()
                if (error is OnnxModelException) throw error
                throw OnnxModelException(
                    code = OnnxModelErrorCode.MODEL_INITIALIZATION_FAILED,
                    message = "Could not inspect ONNX model contract: ${validatedFile.absolutePath}",
                    cause = error,
                )
            }

            return OnnxModelSession(session)
        }

        private const val ALLOW_INTRA_OP_SPINNING_KEY = "session.intra_op.allow_spinning"
        private const val XNNPACK_INTRA_OP_THREADS_KEY = "intra_op_num_threads"
    }
}

private fun modelInitializationFailure(
    modelFile: File,
    cause: Throwable,
): OnnxModelException = OnnxModelException(
    code = OnnxModelErrorCode.MODEL_INITIALIZATION_FAILED,
    message = "Could not initialize ONNX model: ${modelFile.absolutePath}",
    cause = cause,
)

internal fun validateModelFile(modelFile: File): File {
    val absoluteFile = modelFile.absoluteFile
    if (!absoluteFile.exists()) {
        throw OnnxModelException(
            code = OnnxModelErrorCode.MODEL_NOT_FOUND,
            message = "ONNX model file was not found: ${absoluteFile.path}",
        )
    }
    if (!absoluteFile.isFile || !absoluteFile.canRead()) {
        throw OnnxModelException(
            code = OnnxModelErrorCode.MODEL_NOT_READABLE,
            message = "ONNX model path is not a readable file: ${absoluteFile.path}",
        )
    }
    if (absoluteFile.length() == 0L) {
        throw OnnxModelException(
            code = OnnxModelErrorCode.MODEL_INITIALIZATION_FAILED,
            message = "ONNX model file is empty: ${absoluteFile.path}",
        )
    }
    return absoluteFile
}

private fun validateTensorNodes(
    nodes: Map<String, NodeInfo>,
    errorCode: OnnxModelErrorCode,
    nodeKind: String,
) {
    if (nodes.isEmpty()) {
        throw OnnxModelException(
            code = errorCode,
            message = "ONNX model has no $nodeKind nodes.",
        )
    }
    val nonTensorNodes = nodes
        .filterValues { it.info !is TensorInfo }
        .keys
    if (nonTensorNodes.isNotEmpty()) {
        throw OnnxModelException(
            code = errorCode,
            message = "ONNX model has non-tensor $nodeKind nodes: ${nonTensorNodes.joinToString()}.",
        )
    }
}

internal class CloseOnce(
    private val closeAction: () -> Unit,
) {
    private val closed = AtomicBoolean(false)

    val isClosed: Boolean
        get() = closed.get()

    fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAction()
        }
    }
}
