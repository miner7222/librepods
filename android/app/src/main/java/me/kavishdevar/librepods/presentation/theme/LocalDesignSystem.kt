package me.kavishdevar.librepods.presentation.theme

import androidx.compose.runtime.compositionLocalOf

enum class DesignSystem {
    Apple,
    Material
}

val LocalDesignSystem = compositionLocalOf {
    DesignSystem.Apple
}

/**
 * Whether the app is drawing dark, which is not always what the system is doing:
 * the appearance setting can override it. Components must read this rather than
 * calling isSystemInDarkTheme(), or they keep following the system while the rest
 * of the app has already switched.
 */
val LocalIsDarkTheme = compositionLocalOf { false }
