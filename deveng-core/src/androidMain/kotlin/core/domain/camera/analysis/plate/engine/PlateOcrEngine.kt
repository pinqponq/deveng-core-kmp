package core.domain.camera.analysis.plate.engine

import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.ocr.model.FastPlateOcrResult
import core.domain.camera.analysis.ocr.roi.OcrBboxPadding

internal interface PlateOcrEngine : AutoCloseable {
    fun recognize(
        frame: AndroidCameraFrame,
        boundingBox: PlateBoundingBox,
        padding: OcrBboxPadding,
    ): FastPlateOcrResult

    fun warmUp() {}
}