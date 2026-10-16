package core.domain.camera.analysis.ocr.model

import core.domain.camera.analysis.onnx.OnnxModelMetadata
import core.domain.camera.analysis.onnx.ResolvedModelContract
import core.domain.camera.analysis.onnx.ResolvedTensorContract

/** Resolves the technical ONNX shape contract for the FastPlateOCR profile. */
internal object FastPlateOcrModelProfile {

    fun resolve(metadata: OnnxModelMetadata): ResolvedModelContract {
        val input = metadata.inputs.first()
        val output = metadata.outputs.first()
        val resolvedInputShape = input.shape.mapIndexed { index, dim -> if (index == 0) 1L else dim }
        val resolvedOutputShape = output.shape.mapIndexed { index, dim -> if (index == 0) 1L else dim }

        return ResolvedModelContract(
            profileName = "FastPlateOCR",
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
}
