package core.domain.camera.compose

import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.ui.CameraIcons
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraChipModelsTest {

    @Test
    fun backCameraZoomChips_withMaxZoom10_listsFourStopsWithLabels() {
        val chips = backCameraZoomChips(zoomLevel = 1f, cameraDeviceType = CameraDeviceType.DEFAULT, maxZoom = 10f)

        assertEquals(listOf(0.5f, 1f, 2f, 10f), chips.map { it.zoomLevel })
        assertEquals(listOf("0.5x", "1x", "2x", "10x"), chips.map { it.label })
    }

    @Test
    fun backCameraZoomChips_withMaxZoomBelow2_listsOnly05And1() {
        val chips = backCameraZoomChips(zoomLevel = 1f, cameraDeviceType = CameraDeviceType.DEFAULT, maxZoom = 1.5f)

        assertEquals(listOf(0.5f, 1f), chips.map { it.zoomLevel })
    }

    @Test
    fun backCameraZoomChips_withMaxZoomBetween2And10_usesMaxZoomAsLastStop() {
        val chips = backCameraZoomChips(zoomLevel = 1f, cameraDeviceType = CameraDeviceType.DEFAULT, maxZoom = 6.5f)

        assertEquals(listOf("0.5x", "1x", "2x", "6.5x"), chips.map { it.label })
    }

    @Test
    fun backCameraZoomChips_withMaxZoomExactly2_listsTwoOnce() {
        val chips = backCameraZoomChips(zoomLevel = 1f, cameraDeviceType = CameraDeviceType.DEFAULT, maxZoom = 2f)

        assertEquals(listOf(0.5f, 1f, 2f), chips.map { it.zoomLevel })
    }

    @Test
    fun backCameraZoomChips_nearAStop_selectsOnlyThatStop() {
        assertEquals(listOf("2x"), selectedLabels(zoomLevel = 2.1f))
        assertEquals(listOf("2x"), selectedLabels(zoomLevel = 1.9f))
        assertEquals(listOf("1x"), selectedLabels(zoomLevel = 1f))
    }

    @Test
    fun backCameraZoomChips_betweenStops_selectsNothing() {
        assertEquals(emptyList(), selectedLabels(zoomLevel = 1.5f))
    }

    @Test
    fun backCameraZoomChips_belowUltraWideThreshold_selects05() {
        assertEquals(listOf("0.5x"), selectedLabels(zoomLevel = 0.7f))
    }

    @Test
    fun backCameraZoomChips_onUltraWideCamera_selects05WhateverTheZoom() {
        val chips = backCameraZoomChips(zoomLevel = 1f, cameraDeviceType = CameraDeviceType.ULTRA_WIDE, maxZoom = 10f)

        assertEquals(listOf("0.5x"), chips.filter { it.isSelected }.map { it.label })
    }

    @Test
    fun frontCameraModeChips_listsPersonThenGroupAndSelectsTheCurrentMode() {
        val normalSelfie = frontCameraModeChips(isWideSelfie = false)
        val wideSelfie = frontCameraModeChips(isWideSelfie = true)

        assertEquals(
            listOf(
                FrontCameraModeChip(isWideSelfie = false, icon = CameraIcons.person, description = "Person", isSelected = true),
                FrontCameraModeChip(isWideSelfie = true, icon = CameraIcons.group, description = "Group", isSelected = false),
            ),
            normalSelfie,
        )
        assertEquals(listOf(false, true), wideSelfie.map { it.isSelected })
    }

    private fun selectedLabels(zoomLevel: Float): List<String> =
        backCameraZoomChips(zoomLevel = zoomLevel, cameraDeviceType = CameraDeviceType.DEFAULT, maxZoom = 10f)
            .filter { it.isSelected }
            .map { it.label }
}
