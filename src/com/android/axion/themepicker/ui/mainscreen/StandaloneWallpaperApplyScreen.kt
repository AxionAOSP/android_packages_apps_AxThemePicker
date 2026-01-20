/*
 * Copyright (C) 2025 AxionOS
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
package com.android.axion.themepicker.ui.mainscreen

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.themepicker.R
import com.android.axion.themepicker.data.model.WallpaperInfo
import com.android.axion.themepicker.data.model.EffectConfig
import com.android.axion.themepicker.data.model.WallpaperSettings
import com.android.axion.themepicker.data.model.ZoomProperties
import com.android.axion.themepicker.ui.app.WallpaperMiniPreviewsHeight
import com.android.axion.themepicker.ui.app.WallpaperMiniPreviewsWidth
import com.android.axion.themepicker.ui.components.FooterCard
import com.android.axion.themepicker.ui.expressive.ExpressiveHeader
import com.android.axion.themepicker.ui.lockscreen.LockscreenPreview
import com.android.axion.themepicker.ui.preferences.ToggleButton
import androidx.compose.material3.MaterialTheme
import com.android.axion.themepicker.utils.math.scaleRatio
import com.android.axion.themepicker.utils.wallpaper.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneWallpaperApplyScreen(
    wallpaper: WallpaperInfo,
    settings: WallpaperSettings,
    bitmap: Bitmap?,
    onBackClick: () -> Unit,
    onApplyComplete: () -> Unit
) {
    val context = LocalContext.current
    val scale = context.scaleRatio
    val processor = remember { BitmapProcessor(context) }
    val metrics = context.resources.displayMetrics

    var lockscreenSelected by remember { mutableStateOf(settings.lockscreen) }
    var homescreenSelected by remember { mutableStateOf(settings.homescreen) }
    var atmosphereEnabled by remember { mutableStateOf(settings.atmosphere) }
    var glassEnabled by remember { mutableStateOf(settings.glass) }
    var zoomSettings by remember { mutableStateOf(settings.zoomProperties) }
    var isApplying by remember { mutableStateOf(false) }

    val baseBitmap = remember(wallpaper.id, bitmap) { bitmap }

    val currentSystemWallpaper = remember { getCurrentWallpaperBitmap(context, true) }
    val currentLockScreenWallpaper = remember { getCurrentWallpaperBitmap(context, false) }

    val homeSource by remember(baseBitmap, homescreenSelected) {
        derivedStateOf { if (homescreenSelected) baseBitmap else currentSystemWallpaper }
    }
    val lockSource by remember(baseBitmap, lockscreenSelected) {
        derivedStateOf { if (lockscreenSelected) baseBitmap else currentLockScreenWallpaper }
    }

    val homeBitmap by remember(homeSource, zoomSettings) {
        derivedStateOf {
            homeSource?.let {
                if (zoomSettings.isZoomed())
                    applyZoomToBitmap(it, zoomSettings, metrics.widthPixels, metrics.heightPixels)
                else it
            }
        }
    }
    val lockBitmap by remember(lockSource, zoomSettings) {
        derivedStateOf {
            lockSource?.let {
                if (zoomSettings.isZoomed())
                    applyZoomToBitmap(it, zoomSettings, metrics.widthPixels, metrics.heightPixels)
                else it
            }
        }
    }

    val lockscreenBitmap by remember(lockBitmap, atmosphereEnabled, glassEnabled, lockscreenSelected, zoomSettings) {
        derivedStateOf {
            if (lockscreenSelected) lockBitmap?.let { bitmap ->
                processor.processBitmap(bitmap, EffectConfig(atmosphereEnabled, glassEnabled), "lock_${atmosphereEnabled}_${glassEnabled}_${zoomSettings.scale}")
            } else lockSource
        }
    }

    val homescreenBitmap by remember(homeBitmap, atmosphereEnabled, glassEnabled, homescreenSelected, zoomSettings) {
        derivedStateOf {
            if (homescreenSelected) homeBitmap?.let { bitmap ->
                processor.processBitmap(bitmap, EffectConfig(atmosphereEnabled, glassEnabled), "home_${atmosphereEnabled}_${glassEnabled}_${zoomSettings.scale}")
            } else homeSource
        }
    }

    val titleText = when {
        !homescreenSelected && !lockscreenSelected -> stringResource(R.string.select_at_least_one)
        homescreenSelected && lockscreenSelected -> stringResource(R.string.set_wallpaper_on)
        homescreenSelected -> stringResource(R.string.home_screen)
        lockscreenSelected -> stringResource(R.string.lock_screen_label)
        else -> stringResource(R.string.set_wallpaper)
    }

    DisposableEffect(Unit) { onDispose { processor.clearCache() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        ExpressiveHeader(
            title = titleText,
            onBackClick = onBackClick,
            onActionClick = {
                if (!isApplying) {
                    isApplying = true
                    
                    Thread {
                        try {
                            applyWallpaper(
                                context = context,
                                lockscreenBitmap = lockscreenBitmap,
                                homescreenBitmap = homescreenBitmap,
                                lockscreenSelected = lockscreenSelected,
                                homescreenSelected = homescreenSelected
                            )
                            
                            (context as? Activity)?.runOnUiThread {
                                isApplying = false
                                Toast.makeText(context, context.getString(R.string.wallpaper_applied_successfully), Toast.LENGTH_SHORT).show()
                                onApplyComplete()
                            }
                        } catch (e: Exception) {
                            (context as? Activity)?.runOnUiThread {
                                isApplying = false
                                Toast.makeText(context, context.getString(R.string.failed_to_apply_wallpaper, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.start()
                }
            },
            enabled = homescreenSelected || lockscreenSelected,
            actionIcon = Icons.Default.Check
        )

        Spacer(modifier = Modifier.height(32.dp * scale))

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            WallpaperPreviewBox(
                bitmap = lockscreenBitmap,
                isSelected = lockscreenSelected,
                label = stringResource(R.string.lock_screen_label),
                onClick = { lockscreenSelected = !lockscreenSelected },
                isLockscreen = true,
                modifier = Modifier.size(WallpaperMiniPreviewsWidth * scale, WallpaperMiniPreviewsHeight * scale)
            )

            WallpaperPreviewBox(
                bitmap = homescreenBitmap,
                isSelected = homescreenSelected,
                label = stringResource(R.string.home_screen),
                onClick = { homescreenSelected = !homescreenSelected },
                isLockscreen = false,
                modifier = Modifier.size(WallpaperMiniPreviewsWidth * scale, WallpaperMiniPreviewsHeight * scale)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        WallpaperZoomIndicator(zoomSettings)

        val padding = 16.dp * scale
        Box(
            modifier = Modifier.fillMaxWidth().padding(start = padding, end = padding, top = padding),
            contentAlignment = Alignment.Center
        ) {
            FooterCard(
                title = stringResource(id = R.string.effects_beta_notice_title),
                description = stringResource(id = R.string.effects_beta_notice_desc),
                modifier = Modifier.wrapContentWidth()
            )
        }

        WallpaperEffectToggles(
            atmosphereEnabled = atmosphereEnabled,
            glassEnabled = glassEnabled,
            onAtmosphereToggle = { atmosphereEnabled = !atmosphereEnabled },
            onGlassToggle = { glassEnabled = !glassEnabled }
        )

        ApplyingWallpaperDialog(isApplying)
    }
}
