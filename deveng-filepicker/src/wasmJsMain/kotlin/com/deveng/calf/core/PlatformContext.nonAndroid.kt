package com.deveng.calf.core

actual abstract class PlatformContext private constructor() {
    companion object {
        val INSTANCE = object : PlatformContext() {}
    }
}
