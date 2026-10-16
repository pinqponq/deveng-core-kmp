package core.domain.camera.analysis.nanodet.model

import core.domain.camera.analysis.onnx.OnnxModelMetadata
import core.domain.camera.analysis.onnx.ResolvedModelContract
import core.domain.camera.analysis.onnx.ResolvedTensorContract

/** Resolves the technical ONNX shape contract for the Android NanoDet profile. */
internal object NanoDetModelProfile {

    fun resolve(metadata: OnnxModelMetadata): ResolvedModelContract {
        val input = metadata.inputs.first()
        val output = metadata.outputs.first()
        val resolvedInputShape = input.shape.mapIndexed { index, dim -> if (index == 0) 1L else dim }
        val resolvedOutputShape = output.shape.mapIndexed { index, dim -> if (index == 0) 1L else dim }

        return ResolvedModelContract(
            profileName = "NanoDet",
            input = ResolvedTensorContract(
                name = input.name,
                type = input.type,
                metadataShape = input.shape,
                resolvedShape = resolvedInputShape
            ),
            output = ResolvedTensorContract(
                name = output.name,
                type = output.type,
                metadataShape = output.shape,
                resolvedShape = resolvedOutputShape
            )
        )
    }

    fun calculatePredictionCount(width: Int, height: Int): Int {
        return NanoDetModelContract.strides.sumOf { stride ->
            calculateGridDimension(width, stride) * calculateGridDimension(height, stride)
        }
    }

    private fun calculateGridDimension(size: Int, stride: Int): Int = (size + stride - 1) / stride
}
