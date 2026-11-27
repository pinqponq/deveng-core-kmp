package core.domain.camera.ui



import global.deveng.deveng_core.generated.resources.Res

import global.deveng.deveng_core.generated.resources.shared_ic_photo_library

import global.deveng.deveng_core.generated.resources.shared_ic_flash

import global.deveng.deveng_core.generated.resources.shared_ic_cameraswitch

import global.deveng.deveng_core.generated.resources.shared_ic_sun

import global.deveng.deveng_core.generated.resources.shared_ic_moon

import global.deveng.deveng_core.generated.resources.shared_ic_person

import global.deveng.deveng_core.generated.resources.shared_ic_group

import global.deveng.deveng_core.generated.resources.shared_ic_grid

import org.jetbrains.compose.resources.DrawableResource



/**

 * Shared drawable resources for camera UI (e.g. sample app).

 * Use these so the app does not depend on resource accessors from another module.

 */

object CameraIcons {

    val galleryPhotoLibrary: DrawableResource get() = Res.drawable.shared_ic_photo_library

    val flash: DrawableResource get() = Res.drawable.shared_ic_flash

    val grid: DrawableResource get() = Res.drawable.shared_ic_grid

    val switchCamera: DrawableResource get() = Res.drawable.shared_ic_cameraswitch

    val sun: DrawableResource get() = Res.drawable.shared_ic_sun

    val moon: DrawableResource get() = Res.drawable.shared_ic_moon

    val person: DrawableResource get() = Res.drawable.shared_ic_person

    val group: DrawableResource get() = Res.drawable.shared_ic_group

}

