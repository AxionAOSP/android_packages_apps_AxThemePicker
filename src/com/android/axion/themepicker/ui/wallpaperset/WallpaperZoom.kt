/*
 * Copyright 2025-2026 AxionOS
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
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.themepicker.ui.wallpaperset

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.android.axion.compose.preferences.rememberSecureSettingBoolean
import com.android.axion.themepicker.utils.wallpaper.getSystemWallpaperMaxScale

internal const val WALLPAPER_ZOOM_DISABLED_SETTING = "pref_disable_wallpaper_zoom"

@Composable
internal fun rememberWallpaperZoomScale(): Float {
    val context = LocalContext.current
    val zoomDisabled = rememberSecureSettingBoolean(WALLPAPER_ZOOM_DISABLED_SETTING)
    val maxScale = remember(context) { getSystemWallpaperMaxScale(context) }
    val targetScale = if (zoomDisabled) 1f else maxScale
    val scale by
        animateFloatAsState(
            targetValue = targetScale,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "wallpaperZoomScale",
        )
    return scale.coerceIn(minOf(1f, maxScale), maxOf(1f, maxScale))
}
