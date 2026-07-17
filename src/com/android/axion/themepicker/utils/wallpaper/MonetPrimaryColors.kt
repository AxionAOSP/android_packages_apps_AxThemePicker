/*
 * Copyright (C) 2025-2026 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.axion.themepicker.utils.wallpaper

import android.app.WallpaperColors
import android.content.Context
import android.content.theming.ThemeStyle as SystemThemeStyle
import android.graphics.Bitmap
import android.graphics.Color as GraphicsColor
import androidx.compose.material3.ColorScheme as MaterialColorScheme
import androidx.compose.ui.graphics.Color
import com.android.axion.themepicker.utils.settings.loadCurrentSettings
import com.android.internal.graphics.ColorUtils
import com.android.internal.graphics.cam.Cam
import com.android.systemui.monet.ColorScheme as MonetColorScheme
import com.google.ux.material.libmonet.dynamiccolor.DynamicColor
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors

data class MonetPrimaryColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surfaceBright: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
) {
    fun applyTo(colors: MaterialColorScheme): MaterialColorScheme =
        colors.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            surfaceBright = surfaceBright,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
        )
}

fun monetPrimaryColors(context: Context, bitmap: Bitmap, darkTheme: Boolean): MonetPrimaryColors {
    val wallpaperColors = WallpaperColors.fromBitmap(bitmap)
    val seed = MonetColorScheme.getSeedColor(wallpaperColors)
    val settings = loadCurrentSettings(context, Color(seed))
    val style =
        if (settings.fidelity) {
            SystemThemeStyle.CONTENT
        } else {
            SystemThemeStyle.valueOf(settings.style.systemValue)
        }
    val scheme =
        MonetColorScheme(seed, darkTheme, style, settings.contrastLevel.toDouble()).materialScheme
    val dynamicColors = MaterialDynamicColors()
    val color = { dynamicColor: DynamicColor ->
        boostedColor(dynamicColor.getArgb(scheme), settings.chromaBoost)
    }
    return MonetPrimaryColors(
        primary = color(dynamicColors.primary()),
        onPrimary = color(dynamicColors.onPrimary()),
        primaryContainer = color(dynamicColors.primaryContainer()),
        onPrimaryContainer = color(dynamicColors.onPrimaryContainer()),
        surfaceBright = color(dynamicColors.surfaceBright()),
        surfaceContainer = color(dynamicColors.surfaceContainer()),
        surfaceContainerHigh = color(dynamicColors.surfaceContainerHigh()),
        onSurface = color(dynamicColors.onSurface()),
        onSurfaceVariant = color(dynamicColors.onSurfaceVariant()),
    )
}

private fun boostedColor(argb: Int, chromaBoost: Float): Color {
    val cam = Cam.fromInt(argb)
    val chroma = (cam.chroma * (1f + chromaBoost / 100f)).coerceAtMost(150f)
    val boosted = ColorUtils.CAMToColor(cam.hue, chroma, cam.j)
    return Color(ColorUtils.setAlphaComponent(boosted, GraphicsColor.alpha(argb)))
}
