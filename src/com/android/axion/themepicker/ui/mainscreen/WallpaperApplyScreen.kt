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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.widget.Toast
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.axion.themepicker.R
import com.android.axion.themepicker.data.model.WallpaperInfo
import com.android.axion.themepicker.data.model.EffectConfig
import com.android.axion.themepicker.data.model.Screen
import com.android.axion.themepicker.data.model.WallpaperSettings
import com.android.axion.themepicker.data.model.ZoomProperties
import com.android.axion.themepicker.ui.app.WallpaperMiniPreviewsHeight
import com.android.axion.themepicker.ui.app.WallpaperMiniPreviewsWidth
import com.android.axion.themepicker.ui.components.FooterCard
import com.android.axion.themepicker.ui.expressive.ExpressiveHeader
import com.android.axion.themepicker.ui.lockscreen.LockscreenPreview
import com.android.axion.themepicker.ui.preferences.ToggleButton
import com.android.axion.themepicker.ui.preview.HomescreenPreview
import androidx.compose.material3.MaterialTheme
import com.android.axion.themepicker.utils.math.scaleRatio
import com.android.axion.themepicker.utils.math.sdp
import com.android.axion.themepicker.utils.wallpaper.applyZoomToBitmap
import com.android.axion.themepicker.utils.wallpaper.centerCrop
import com.android.axion.themepicker.utils.wallpaper.getWallpaperDrawable
import com.android.axion.themepicker.utils.wallpaper.getCurrentWallpaperBitmap
import com.android.axion.themepicker.utils.wallpaper.BitmapProcessor
import com.android.axion.themepicker.viewmodel.MainScreenViewModel
import com.android.axion.themepicker.viewmodel.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperApplyScreen(
    wallpaper: WallpaperInfo,
    settings: WallpaperSettings,
    bitmap: Bitmap?,
    wallpaperViewModel: WallpaperViewModel = viewModel(),
    mainScreenViewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val scale = context.scaleRatio
    val processor = remember { BitmapProcessor(context) }
    val metrics = context.resources.displayMetrics

    var lockscreenSelected by rememberSaveable { mutableStateOf(settings.lockscreen) }
    var homescreenSelected by rememberSaveable { mutableStateOf(settings.homescreen) }
    var atmosphereEnabled by rememberSaveable { mutableStateOf(settings.atmosphere) }
    var glassEnabled by rememberSaveable { mutableStateOf(settings.glass) }
    var zoomSettings by rememberSaveable { mutableStateOf(settings.zoomProperties) }
    var isApplying by remember { mutableStateOf(false) }

    val drawable = getWallpaperDrawable(context, wallpaper.drawableRes)
    val baseBitmap = remember(wallpaper.id) {
        if (wallpaper.drawableRes == -1) {
            bitmap
        } else {
            drawable?.toBitmap()
        }
    }

    val currentSystemWallpaper = remember { getCurrentWallpaperBitmap(context, true) }
    val currentLockScreenWallpaper = remember { getCurrentWallpaperBitmap(context, false) }

    val homeSource = remember(baseBitmap, homescreenSelected) {
        if (homescreenSelected) baseBitmap else currentSystemWallpaper
    }

    val lockSource = remember(baseBitmap, lockscreenSelected) {
        if (lockscreenSelected) baseBitmap else currentLockScreenWallpaper
    }

    val homeBitmap = remember(homeSource, zoomSettings) {
        homeSource?.let {
            if (zoomSettings.isZoomed()) {
                applyZoomToBitmap(
                    it,
                    zoomSettings,
                    metrics.widthPixels,
                    metrics.heightPixels
                )
            } else {
                it
            }
        }
    }

    val lockBitmap = remember(lockSource, zoomSettings) {
        lockSource?.let {
            if (zoomSettings.isZoomed()) {
                applyZoomToBitmap(
                    it,
                    zoomSettings,
                    metrics.widthPixels,
                    metrics.heightPixels
                )
            } else {
                it
            }
        }
    }

    val lockscreenBitmap = remember(lockBitmap, atmosphereEnabled, glassEnabled, lockscreenSelected, zoomSettings) {
        if (lockscreenSelected) {
            lockBitmap?.let { bitmap ->
                val effect = EffectConfig(atmosphere = atmosphereEnabled, glass = glassEnabled)
                processor.processBitmap(bitmap, effect, "lock_${glassEnabled}_${atmosphereEnabled}_${zoomSettings.scale}")
            }
        } else {
            lockSource
        }
    }

    val homescreenBitmap = remember(homeBitmap, atmosphereEnabled, glassEnabled, homescreenSelected, zoomSettings) {
        if (homescreenSelected) {
            homeBitmap?.let { bitmap ->
                val effect = EffectConfig(atmosphere = atmosphereEnabled, glass = glassEnabled)
                processor.processBitmap(
                    bitmap,
                    effect,
                    "home_${atmosphereEnabled}_${glassEnabled}_${zoomSettings.scale}"
                )
            }
        } else {
            homeSource
        }
    }
    
    val titleText = when {
        !homescreenSelected && !lockscreenSelected -> stringResource(R.string.select_at_least_one)
        homescreenSelected && lockscreenSelected -> stringResource(R.string.set_wallpaper_on)
        homescreenSelected -> stringResource(R.string.home_screen)
        lockscreenSelected -> stringResource(R.string.lock_screen_label)
        else -> stringResource(R.string.set_wallpaper)
    }

    DisposableEffect(Unit) {
        onDispose { processor.clearCache() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        ExpressiveHeader(
            title = titleText, 
            onBackClick = { mainScreenViewModel.goBack() },
            onActionClick = {
                isApplying = true

                val updated = WallpaperSettings(
                    wallpaperId = wallpaper.drawableRes,
                    lockscreen = lockscreenSelected,
                    homescreen = homescreenSelected,
                    atmosphere = if (homescreenSelected || lockscreenSelected) atmosphereEnabled else false,
                    glass = if (lockscreenSelected || homescreenSelected) glassEnabled else false,
                    zoomProperties = if (lockscreenSelected || homescreenSelected) zoomSettings else ZoomProperties()
                )

                wallpaperViewModel.applyNewWallpaper(
                    context = context,
                    lockscreenBitmap = lockscreenBitmap,
                    homescreenBitmap = homescreenBitmap,
                    lockscreenSelected = lockscreenSelected,
                    homescreenSelected = homescreenSelected
                ) {
                    isApplying = false
                    Toast.makeText(context, context.getString(R.string.wallpaper_applied_successfully), Toast.LENGTH_SHORT)
                        .show()
                    wallpaperViewModel.updateSettings(updated)
                    mainScreenViewModel.resetToMain()
                }
            },
            enabled = homescreenSelected || lockscreenSelected,
            actionIcon = Icons.Default.Check
        )

        Spacer(modifier = Modifier.height(32.sdp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            WallpaperPreviewBox(
                bitmap = lockscreenBitmap,
                isSelected = lockscreenSelected,
                label = stringResource(R.string.lock_screen_label),
                onClick = { lockscreenSelected = !lockscreenSelected },
                isLockscreen = true,
                modifier = Modifier.size(
                    WallpaperMiniPreviewsWidth * scale,
                    WallpaperMiniPreviewsHeight  * scale
                )
            )

            WallpaperPreviewBox(
                bitmap = homescreenBitmap,
                isSelected = homescreenSelected,
                label = stringResource(R.string.home_screen),
                onClick = { homescreenSelected = !homescreenSelected },
                isLockscreen = false,
                modifier = Modifier.size(
                    WallpaperMiniPreviewsWidth * scale,
                    WallpaperMiniPreviewsHeight * scale
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        WallpaperZoomIndicator(zoomSettings)
        
        val padding = 16.sdp
        
        Box(
            modifier = Modifier
                .fillMaxWidth() 
                .padding(start = padding, end = padding, top = padding),
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

@Composable
fun WallpaperPreviewBox(
    bitmap: Bitmap?,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLockscreen: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) colors.primary else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(13.dp))
                ) {
                    if (isLockscreen) {
                        LockscreenPreview(
                            wallpaperBitmap = bitmap,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(13.dp)),
                            isPreview = true
                        )
                    } else {
                        Image(
                            painter = BitmapPainter(bitmap.asImageBitmap()),
                            contentDescription = stringResource(R.string.preview_label, label),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(13.dp)),
                            contentScale = ContentScale.Crop
                        )

                        HomescreenPreview(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(13.dp)),
                            wallpaperDrawable = BitmapDrawable(
                                context.resources,
                                centerCrop(context, bitmap)
                            )
                        )
                    }

                    if (!isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface
        )
    }
}

@Composable
fun WallpaperZoomIndicator(zoomProperties: ZoomProperties) {
    val colors = MaterialTheme.colorScheme
    val scale = LocalContext.current.scaleRatio

    if (zoomProperties.isZoomed()) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.sdp, vertical = 8.sdp)
        ) {
            Surface(
                color = colors.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.sdp, vertical = 6.sdp)
                ) {
                    Icon(
                        Icons.Default.CropFree,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.cropped_percentage, (zoomProperties.scale * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperEffectToggles(
    atmosphereEnabled: Boolean,
    glassEnabled: Boolean,
    onAtmosphereToggle: () -> Unit,
    onGlassToggle: () -> Unit
) {
    val scale = LocalContext.current.scaleRatio
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.sdp, vertical = 32.sdp)
    ) {
        ToggleButton(
            icon = Icons.Default.WaterDrop,
            label = stringResource(R.string.atmosphere),
            enabled = atmosphereEnabled,
            onClick = onAtmosphereToggle,
        )

        ToggleButton(
            icon = Icons.Default.HeatPump,
            label = stringResource(R.string.glass),
            enabled = glassEnabled,
            onClick = onGlassToggle,
        )
    }
}

@Composable
fun ApplyingWallpaperDialog(isApplying: Boolean) {
    if (isApplying) {
        AlertDialog(
            onDismissRequest = {},
            title = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.setting_wallpaper))
                }
            },
            confirmButton = {}
        )
    }
}
