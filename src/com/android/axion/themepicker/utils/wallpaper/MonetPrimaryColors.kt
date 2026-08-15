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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.axion.themepicker.utils.settings.loadCurrentSettings
import com.android.internal.graphics.ColorUtils
import com.android.internal.graphics.cam.Cam
import com.android.systemui.monet.ColorScheme as MonetColorScheme
import com.google.ux.material.libmonet.dynamiccolor.DynamicColor
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors

typealias MonetPrimaryColors = MaterialColorScheme

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
    val baseColors = if (darkTheme) darkColorScheme() else lightColorScheme()
    return baseColors.copy(
        primary = color(dynamicColors.primary()),
        onPrimary = color(dynamicColors.onPrimary()),
        primaryContainer = color(dynamicColors.primaryContainer()),
        onPrimaryContainer = color(dynamicColors.onPrimaryContainer()),
        inversePrimary = color(dynamicColors.inversePrimary()),
        secondary = color(dynamicColors.secondary()),
        onSecondary = color(dynamicColors.onSecondary()),
        secondaryContainer = color(dynamicColors.secondaryContainer()),
        onSecondaryContainer = color(dynamicColors.onSecondaryContainer()),
        tertiary = color(dynamicColors.tertiary()),
        onTertiary = color(dynamicColors.onTertiary()),
        tertiaryContainer = color(dynamicColors.tertiaryContainer()),
        onTertiaryContainer = color(dynamicColors.onTertiaryContainer()),
        background = color(dynamicColors.background()),
        onBackground = color(dynamicColors.onBackground()),
        surface = color(dynamicColors.surface()),
        onSurface = color(dynamicColors.onSurface()),
        surfaceVariant = color(dynamicColors.surfaceVariant()),
        onSurfaceVariant = color(dynamicColors.onSurfaceVariant()),
        surfaceTint = color(dynamicColors.surfaceTint()),
        inverseSurface = color(dynamicColors.inverseSurface()),
        inverseOnSurface = color(dynamicColors.inverseOnSurface()),
        error = color(dynamicColors.error()),
        onError = color(dynamicColors.onError()),
        errorContainer = color(dynamicColors.errorContainer()),
        onErrorContainer = color(dynamicColors.onErrorContainer()),
        outline = color(dynamicColors.outline()),
        outlineVariant = color(dynamicColors.outlineVariant()),
        scrim = color(dynamicColors.scrim()),
        surfaceBright = color(dynamicColors.surfaceBright()),
        surfaceDim = color(dynamicColors.surfaceDim()),
        surfaceContainer = color(dynamicColors.surfaceContainer()),
        surfaceContainerHigh = color(dynamicColors.surfaceContainerHigh()),
        surfaceContainerHighest = color(dynamicColors.surfaceContainerHighest()),
        surfaceContainerLow = color(dynamicColors.surfaceContainerLow()),
        surfaceContainerLowest = color(dynamicColors.surfaceContainerLowest()),
        primaryFixed = color(dynamicColors.primaryFixed()),
        primaryFixedDim = color(dynamicColors.primaryFixedDim()),
        onPrimaryFixed = color(dynamicColors.onPrimaryFixed()),
        onPrimaryFixedVariant = color(dynamicColors.onPrimaryFixedVariant()),
        secondaryFixed = color(dynamicColors.secondaryFixed()),
        secondaryFixedDim = color(dynamicColors.secondaryFixedDim()),
        onSecondaryFixed = color(dynamicColors.onSecondaryFixed()),
        onSecondaryFixedVariant = color(dynamicColors.onSecondaryFixedVariant()),
        tertiaryFixed = color(dynamicColors.tertiaryFixed()),
        tertiaryFixedDim = color(dynamicColors.tertiaryFixedDim()),
        onTertiaryFixed = color(dynamicColors.onTertiaryFixed()),
        onTertiaryFixedVariant = color(dynamicColors.onTertiaryFixedVariant()),
    )
}

private fun boostedColor(argb: Int, chromaBoost: Float): Color {
    val cam = Cam.fromInt(argb)
    val chroma = (cam.chroma * (1f + chromaBoost / 100f)).coerceAtMost(150f)
    val boosted = ColorUtils.CAMToColor(cam.hue, chroma, cam.j)
    return Color(ColorUtils.setAlphaComponent(boosted, GraphicsColor.alpha(argb)))
}
