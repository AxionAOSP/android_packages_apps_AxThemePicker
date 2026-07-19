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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.themepicker.ui.preview

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.components.WallpaperTargetDialog
import com.android.axion.themepicker.ui.wallpaperset.rememberWallpaperZoomScale
import com.android.axion.themepicker.utils.wallpaper.DisplayHelper
import com.android.axion.themepicker.utils.wallpaper.MonetPrimaryColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RESULT_DISPLAY_MS = 1500L

@Composable
fun WallpaperPreviewScreen(
    wallpaperBitmap: Bitmap?,
    primaryColors: MonetPrimaryColors?,
    targetFlags: Int = 0,
    onApply: (Bitmap, Int) -> Unit,
    onBack: () -> Unit,
    onApplySuccess: () -> Unit = onBack,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallpaperZoomScale = rememberWallpaperZoomScale()

    val imageBitmap = remember(wallpaperBitmap) { wallpaperBitmap?.asImageBitmap() }
    val hasMultiDisplay = remember { DisplayHelper.hasMultiInternalDisplays(context) }
    val unfoldedSize = remember { DisplayHelper.getWallpaperDisplaySize(context) }
    val foldedSize = remember {
        if (hasMultiDisplay) DisplayHelper.getRealSize(DisplayHelper.getSmallerDisplay(context))
        else null
    }

    val hasPresetTarget = targetFlags != 0

    var showTargetDialog by remember { mutableStateOf(false) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    var isApplying by remember { mutableStateOf(false) }
    var applyResultMessage by remember { mutableStateOf<String?>(null) }

    val colors = primaryColors?.applyTo(MaterialTheme.colorScheme) ?: MaterialTheme.colorScheme

    BackHandler(enabled = !isApplying) { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.preview_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isApplying) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surfaceContainer,
                        titleContentColor = colors.onSurface,
                        navigationIconContentColor = colors.onSurface,
                    ),
            )
        },
        containerColor = colors.surfaceContainer,
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (wallpaperBitmap == null) {
                    LoadingIndicator(color = colors.primary)
                } else if (hasMultiDisplay && foldedSize != null) {
                    DualDisplayPreview(
                        wallpaperBitmap = wallpaperBitmap,
                        foldedDisplaySize = foldedSize,
                        unfoldedDisplaySize = unfoldedSize,
                        wallpaperZoomScale = wallpaperZoomScale,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    val aspectRatio = unfoldedSize.x.toFloat() / unfoldedSize.y
                    val shape = MaterialTheme.shapes.largeIncreased

                    Card(
                        modifier = Modifier.fillMaxHeight(0.85f).aspectRatio(aspectRatio),
                        shape = shape,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = colors.surfaceContainerHigh,
                                contentColor = colors.onSurface,
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    ) {
                        imageBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = stringResource(R.string.wallpaper_photo),
                                modifier =
                                    Modifier.fillMaxSize()
                                        .clip(shape)
                                        .graphicsLayer(
                                            scaleX = wallpaperZoomScale,
                                            scaleY = wallpaperZoomScale,
                                        ),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (hasPresetTarget) {
                        wallpaperBitmap?.let { bmp ->
                            isApplying = true
                            scope.launch {
                                withContext(Dispatchers.IO) { onApply(bmp, targetFlags) }
                                isApplying = false
                                applyResultMessage =
                                    context.getString(R.string.wallpaper_set_success)
                                handler.postDelayed({ onApplySuccess() }, RESULT_DISPLAY_MS)
                            }
                        }
                    } else {
                        showTargetDialog = true
                    }
                },
                enabled = wallpaperBitmap != null && !isApplying,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
            ) {
                Text(
                    text = stringResource(R.string.set_wallpaper),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (showTargetDialog) {
        WallpaperTargetDialog(
            colors = colors,
            onDismiss = { showTargetDialog = false },
            onSelect = { flags ->
                showTargetDialog = false
                wallpaperBitmap?.let { bmp ->
                    isApplying = true
                    scope.launch {
                        withContext(Dispatchers.IO) { onApply(bmp, flags) }
                        isApplying = false
                        applyResultMessage = context.getString(R.string.wallpaper_set_success)
                        handler.postDelayed({ onApplySuccess() }, RESULT_DISPLAY_MS)
                    }
                }
            },
        )
    }

    ApplyingWallpaperDialog(
        isApplying = isApplying,
        resultMessage = applyResultMessage,
        colors = colors,
    )
}

@Composable
private fun ApplyingWallpaperDialog(
    isApplying: Boolean,
    resultMessage: String?,
    colors: ColorScheme,
) {
    val showDialog = isApplying || resultMessage != null
    if (!showDialog) return

    BasicAlertDialog(onDismissRequest = {}) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val defaultEffects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val defaultSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
                val fastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                val fastSpatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
                AnimatedContent(
                    targetState = resultMessage != null,
                    transitionSpec = {
                        (fadeIn(defaultEffects) +
                            scaleIn(defaultSpatial, initialScale = 0.6f)) togetherWith
                            (fadeOut(fastEffects) + scaleOut(fastSpatial, targetScale = 0.8f))
                    },
                    label = "applyState",
                ) { hasResult ->
                    if (hasResult) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(48.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                text = resultMessage ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ContainedLoadingIndicator(
                                modifier = Modifier.size(48.dp),
                                containerColor = colors.primaryContainer,
                                indicatorColor = colors.onPrimaryContainer,
                            )
                            Text(
                                text = stringResource(R.string.applying_wallpaper),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
