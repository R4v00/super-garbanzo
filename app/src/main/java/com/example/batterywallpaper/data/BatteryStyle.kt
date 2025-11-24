package com.example.batterywallpaper.data

sealed class BatteryStyle(val id: String) {
    object Cartoonish : BatteryStyle("cartoonish")
    object Futuristic : BatteryStyle("futuristic")

    companion object {
        fun fromId(id: String?): BatteryStyle {
            return when (id) {
                "futuristic" -> Futuristic
                else -> Cartoonish
            }
        }
    }
}