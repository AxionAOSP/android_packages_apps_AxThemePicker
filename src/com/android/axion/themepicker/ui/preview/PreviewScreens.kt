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
package com.android.axion.themepicker.ui.preview

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.*
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.axion.themepicker.R
import com.android.axion.themepicker.data.model.WallpaperInfo
import com.android.axion.themepicker.data.model.EffectConfig
import com.android.axion.themepicker.data.model.WallpaperSettings
import com.android.axion.themepicker.data.model.ZoomProperties
import com.android.axion.themepicker.utils.colors.ColorUtils
import com.android.axion.themepicker.utils.math.scaleRatio
import com.android.axion.themepicker.utils.wallpaper.applyZoomToBitmap
import com.android.axion.themepicker.utils.wallpaper.BitmapProcessor
import com.android.axion.themepicker.utils.wallpaper.centerCrop
import com.android.axion.themepicker.utils.wallpaper.getWallpaperDrawable
import com.android.axion.themepicker.utils.wallpaper.getCurrentWallpaperBitmap
import com.android.axion.themepicker.ui.carousel.WallpaperCarouselCard
import com.android.axion.themepicker.ui.preferences.ToggleButton
import androidx.compose.material3.MaterialTheme
import com.android.axion.themepicker.viewmodel.MainScreenViewModel
import com.android.axion.themepicker.viewmodel.WallpaperViewModel
import kotlin.coroutines.*
import kotlinx.coroutines.*
import kotlin.math.*
import java.io.File
import java.io.FileOutputStream

@Composable
fun WallpaperPreviewScreen(
    settings: WallpaperSettings,
    wallpaper: WallpaperInfo,
    bitmap: Bitmap? = null,
    mainScreenViewModel: MainScreenViewModel = viewModel(),
    wallpaperViewModel: WallpaperViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val isUserSelected = wallpaper.drawableRes == -1
    val context = LocalContext.current
    val drawable = getWallpaperDrawable(context, wallpaper.drawableRes)
    val sourceBitmap = remember(wallpaper.id) {
        if (isUserSelected) {
            bitmap
        } else drawable?.toBitmap() 
    }
    
    val cbitmap = remember(sourceBitmap) {
        centerCrop(context, sourceBitmap)
    }

    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var zoomProperties by remember { mutableStateOf(ZoomProperties()) }

    if (cbitmap == null) return

    CommonWallpaperPreview(
        sourceBitmap = cbitmap,
        zoomProperties = zoomProperties,
        onBack = mainScreenViewModel::goBack,
        onCheck = {
            scope.launch {
                cbitmap?.let { bitmap ->
                    val updatedSettings = settings.copy(
                        zoomProperties = zoomProperties
                    )
                    wallpaperViewModel.updateSettings(updatedSettings)
                    mainScreenViewModel.onApplyConfirmed(
                        wallpaper = wallpaper,
                        zoom = updatedSettings.zoomProperties,
                        bitmap = bitmap
                    )
                }
            }
        },
        onZoomChanged = { zoomProperties = it },
        topEndAction = { overlayColor, contentColor ->
            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .background(overlayColor, CircleShape)
            ) {
                Icon(Icons.Default.Info, contentDescription = stringResource(R.string.info), tint = contentColor)
            }
        }
    )

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            title = { Text(text = stringResource(R.string.wallpaper_info)) },
            text = {
                Column {
                    Text(text = wallpaper.title ?: stringResource(R.string.unknown))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = wallpaper.id, style = MaterialTheme.typography.bodySmall)
                }
            },
            containerColor = colors.surfaceContainerHigh
        )
    }
}

@Composable
fun EditCurrentWallpaperScreen(
    settings: WallpaperSettings,
    mainScreenViewModel: MainScreenViewModel = viewModel(),
    wallpaperViewModel: WallpaperViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentWallpaperTitle = stringResource(R.string.current_wallpaper)
    
    val wallBitmap = getCurrentWallpaperBitmap(context, true)
    
    val cbitmap = remember(wallBitmap) {
        centerCrop(context, wallBitmap)
    }

    var atmosphereEnabled by rememberSaveable { mutableStateOf(settings.atmosphere) }
    var glassEnabled by rememberSaveable { mutableStateOf(settings.glass) }
    var homescreenSelected by rememberSaveable { mutableStateOf(settings.homescreen) }
    var zoomProperties by remember { mutableStateOf(settings.zoomProperties) }

    LaunchedEffect(atmosphereEnabled) {
        if (atmosphereEnabled) {
            homescreenSelected = true
        }
    }

    if (cbitmap == null) return

    CommonWallpaperPreview(
        sourceBitmap = cbitmap,
        zoomProperties = zoomProperties,
        atmosphereEnabled = atmosphereEnabled,
        glassEnabled = glassEnabled,
        onAtmosphereToggle = { atmosphereEnabled = !atmosphereEnabled },
        onGlassToggle = { glassEnabled = !glassEnabled },
        onBack = mainScreenViewModel::goBack,
        onCheck = {
            scope.launch {
                cbitmap?.let { bitmap ->
                    val updatedSettings = settings.copy(
                        atmosphere = atmosphereEnabled,
                        glass = glassEnabled,
                        homescreen = homescreenSelected,
                        zoomProperties = zoomProperties
                    )
                    wallpaperViewModel.updateSettings(updatedSettings)
                    val wallInfo = WallpaperInfo(
                        id = "wallpaper_home",
                        title = currentWallpaperTitle,
                        drawableRes = -1
                    )
                    mainScreenViewModel.onApplyConfirmed(
                        wallpaper = wallInfo,
                        zoom = zoomProperties,
                        bitmap = bitmap
                    )
                }
            }
        },
        onZoomChanged = { zoomProperties = it }
    )
}

@Composable
private fun CommonWallpaperPreview(
    sourceBitmap: Bitmap?,
    zoomProperties: ZoomProperties,
    atmosphereEnabled: Boolean = false,
    glassEnabled: Boolean = false,
    onAtmosphereToggle: () -> Unit = {},
    onGlassToggle: () -> Unit = {},
    onBack: () -> Unit,
    onCheck: () -> Unit,
    onZoomChanged: (ZoomProperties) -> Unit = {},
    topEndAction: @Composable ((Color, Color) -> Unit)? = null
) {
    val context = LocalContext.current
    val ratio = context.scaleRatio
    val activity = context as? Activity

    val statusBarColor = remember { ColorUtils.getStatusBarColor(activity) }
    val contentOnStatusBar = remember(statusBarColor) { ColorUtils.getContentColor(statusBarColor) }
    val overlayColor = remember(statusBarColor) { ColorUtils.createOverlayColor(statusBarColor) }

    var scale by remember { mutableFloatStateOf(zoomProperties.scale) }
    var offset by remember { mutableStateOf(Offset(zoomProperties.offsetX, zoomProperties.offsetY)) }

    LaunchedEffect(scale, offset) {
        if (scale != zoomProperties.scale || 
            offset.x != zoomProperties.offsetX || 
            offset.y != zoomProperties.offsetY) {
            onZoomChanged(ZoomProperties(scale, offset.x, offset.y))
        }
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (sourceBitmap != null) {
            val painter = BitmapPainter(sourceBitmap.asImageBitmap())
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.wallpaper),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            if (newScale > 1f) {
                                val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                val newOffset = offset + pan * newScale
                                offset = Offset(
                                    x = newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                    y = newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                            scale = newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2f
                                    val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                    val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                    offset = Offset(
                                        x = ((size.width / 2 - tapOffset.x) * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = ((size.height / 2 - tapOffset.y) * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                }
                            }
                        )
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.FillBounds
            )

            if (scale > 1f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    drawRect(
                        color = Color.White.copy(alpha = 0.8f),
                        topLeft = Offset(strokeWidth, strokeWidth),
                        size = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2),
                        style = Stroke(width = strokeWidth)
                    )
                    
                    val gridColor = Color.White.copy(alpha = 0.3f)
                    val gridStroke = 1.dp.toPx()
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(size.width / 3, 0f),
                        end = Offset(size.width / 3, size.height),
                        strokeWidth = gridStroke
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(size.width * 2 / 3, 0f),
                        end = Offset(size.width * 2 / 3, size.height),
                        strokeWidth = gridStroke
                    )
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, size.height / 3),
                        end = Offset(size.width, size.height / 3),
                        strokeWidth = gridStroke
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, size.height * 2 / 3),
                        end = Offset(size.width, size.height * 2 / 3),
                        strokeWidth = gridStroke
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp * ratio)
                .padding(top = 48.dp * ratio)
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp * ratio)
                    .background(overlayColor, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = contentOnStatusBar)
            }

            if (topEndAction != null) {
                topEndAction(overlayColor, contentOnStatusBar)
            } else {
                Spacer(modifier = Modifier.size(48.dp * ratio))
            }

            IconButton(
                onClick = onCheck,
                modifier = Modifier
                    .size(48.dp * ratio)
                    .background(overlayColor, CircleShape)
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.apply), tint = contentOnStatusBar)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (scale <= 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                            )
                        )
                        .padding(bottom = 60.dp * ratio),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Pinch,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp * ratio)
                        )
                        Spacer(modifier = Modifier.width(8.dp * ratio))
                        Text(
                            stringResource(R.string.pinch_to_crop),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (scale > 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.percentage_format, (scale * 100).toInt()),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
