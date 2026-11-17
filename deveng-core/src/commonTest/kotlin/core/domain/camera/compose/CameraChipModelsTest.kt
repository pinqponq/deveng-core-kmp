package core.domain.camera.compose

import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.ui.CameraIcons
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraChipModelsTest {

    @Test
    fun backCameraZoomChips_withMaxZoom10_listsFourStops() {
        assertEquals(listOf(0.5f, 1f, 2f, MAX_ZOOM), zoomChips(zoomLevel = 1f).map { it.zoomLevel })
    }

    @Test
    fun backCameraZoomChips_withMaxZoom10_labelsEveryStop() {
        assertEquals(listOf(ZOOM_05_LABEL, ZOOM_1_LABEL, ZOOM_2_LABEL, "10x"), zoomChips(zoomLevel = 1f).map { it.label })
    }

    @Test
    fun backCameraZoomChips_withMaxZoomBelow2_listsOnly05And1() {
        assertEquals(listOf(0.5f, 1f), zoomChips(zoomLevel = 1f, maxZoom = 1.5f).map { it.zoomLevel })
    }

    @Test
    fun backCameraZoomChips_withMaxZoomBetween2And10_usesMaxZoomAsLastStop() {
        assertEquals(listOf(ZOOM_05_LABEL, ZOOM_1_LABEL, ZOOM_2_LABEL, "6.5x"), zoomChips(zoomLevel = 1f, maxZoom = 6.5f).map { it.label })
    }

    @Test
    fun backCameraZoomChips_withMaxZoomExactly2_listsTwoOnce() {
        assertEquals(listOf(0.5f, 1f, 2f), zoomChips(zoomLevel = 1f, maxZoom = 2f).map { it.zoomLevel })
    }

    @Test
    fun backCameraZoomChips_slightlyAboveStop_selectsThatStop() {
        assertEquals(listOf(ZOOM_2_LABEL), selectedLabels(zoomLevel = 2.1f))
    }

    @Test
    fun backCameraZoomChips_slightlyBelowStop_selectsThatStop() {
        assertEquals(listOf(ZOOM_2_LABEL), selectedLabels(zoomLevel = 1.9f))
    }

    @Test
    fun backCameraZoomChips_exactlyOnStop_selectsThatStop() {
        assertEquals(listOf(ZOOM_1_LABEL), selectedLabels(zoomLevel = 1f))
    }

    @Test
    fun backCameraZoomChips_betweenStops_selectsNothing() {
        assertEquals(emptyList(), selectedLabels(zoomLevel = 1.5f))
    }

    @Test
    fun backCameraZoomChips_belowUltraWideThreshold_selects05() {
        assertEquals(listOf(ZOOM_05_LABEL), selectedLabels(zoomLevel = 0.7f))
    }

    @Test
    fun backCameraZoomChips_exactlyAtUltraWideThreshold_selectsNothing() {
        assertEquals(emptyList(), selectedLabels(zoomLevel = 0.75f))
    }

    @Test
    fun backCameraZoomChips_onUltraWideCamera_selects05WhateverTheZoom() {
        val chips = zoomChips(zoomLevel = 1f, cameraDeviceType = CameraDeviceType.ULTRA_WIDE)

        assertEquals(listOf(ZOOM_05_LABEL), chips.filter { it.isSelected }.map { it.label })
    }

    @Test
    fun frontCameraModeChips_normalSelfie_listsSelectedPersonThenGroup() {
        assertEquals(
            listOf(
                FrontCameraModeChip(isWideSelfie = false, icon = CameraIcons.person, description = "Person", isSelected = true),
                FrontCameraModeChip(isWideSelfie = true, icon = CameraIcons.group, description = "Group", isSelected = false),
            ),
            frontCameraModeChips(isWideSelfie = false),
        )
    }

    @Test
    fun frontCameraModeChips_wideSelfie_selectsOnlyGroup() {
        assertEquals(listOf(false, true), frontCameraModeChips(isWideSelfie = true).map { it.isSelected })
    }

    private fun zoomChips(
        zoomLevel: Float,
        cameraDeviceType: CameraDeviceType = CameraDeviceType.DEFAULT,
        maxZoom: Float = MAX_ZOOM,
    ): List<CameraZoomChip> = backCameraZoomChips(zoomLevel = zoomLevel, cameraDeviceType = cameraDeviceType, maxZoom = maxZoom)

    private fun selectedLabels(zoomLevel: Float): List<String> =
        zoomChips(zoomLevel = zoomLevel).filter { it.isSelected }.map { it.label }

    private companion object {
        const val MAX_ZOOM = 10f
        const val ZOOM_05_LABEL = "0.5x"
        const val ZOOM_1_LABEL = "1x"
        const val ZOOM_2_LABEL = "2x"
    }
}
