package core.domain.camera.analysis.plate.engine

import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.ocr.FastPlateOcr
import core.domain.camera.analysis.ocr.FastPlateOcrConfig
import core.domain.camera.analysis.ocr.model.FastPlateOcrResult
import core.domain.camera.analysis.ocr.roi.OcrBboxPadding
import java.io.File

internal class FastPlateOcrEngine(
    modelFile: File,
    config: FastPlateOcrConfig = FastPlateOcrConfig()
) : PlateOcrEngine {
    private val ocr = FastPlateOcr(
        modelFile = modelFile,
        config = config
    )

    override fun recognize(
        frame: AndroidCameraFrame,
        boundingBox: PlateBoundingBox,
        padding: OcrBboxPadding
    ): FastPlateOcrResult {
        return ocr.recognize(
            frame = frame,
            boundingBox = boundingBox,
            padding = padding
        )
    }

    override fun warmUp() {
        ocr.warmUp()
    }

    override fun close() {
        ocr.close()
    }
}
