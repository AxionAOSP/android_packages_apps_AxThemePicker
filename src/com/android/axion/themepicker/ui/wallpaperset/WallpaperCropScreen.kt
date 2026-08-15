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

package com.android.axion.themepicker.ui.wallpaperset

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.compose.preferences.rememberSecureSettingBooleanState
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.components.WallpaperTargetDialog
import com.android.axion.themepicker.utils.wallpaper.MonetPrimaryColors
import com.android.axion.themepicker.utils.wallpaper.getCurrentWallpaperBitmap
import com.android.axion.themepicker.utils.wallpaper.getSystemWallpaperMaxScale
import com.android.axion.themepicker.utils.wallpaper.getWallpaperDrawable
import com.android.axion.themepicker.utils.wallpaper.monetPrimaryColors
import com.android.axion.util.DisplayUtils
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "WallpaperCropScreen"
private const val MAX_ZOOM_FACTOR = 8f

private data class LoadedWallpaper(
    val bitmap: Bitmap,
    val dimensions: Pair<Int, Int>?,
    val fitBackgroundColor: Color,
    val primaryColors: MonetPrimaryColors,
)

@Composable
fun WallpaperCropScreen(
    imageUri: Uri? = null,
    drawableRes: Int = 0,
    targetFlags: Int = 0,
    onNext: ((Bitmap, Map<Point, Rect>) -> Unit)? = null,
    onApply: ((Uri, Rect, Map<Point, Rect>?, Int) -> Unit)? = null,
    onApplyBitmap: ((Bitmap, Int) -> Unit)? = null,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val isStandaloneMode = onApply != null || onApplyBitmap != null
    val showFitModeToggle = isStandaloneMode || onNext != null
    val isLockOnly =
        (targetFlags and WallpaperManager.FLAG_LOCK) != 0 &&
            (targetFlags and WallpaperManager.FLAG_SYSTEM) == 0
    val darkTheme = isSystemInDarkTheme()
    val (wallpaperZoomDisabled, setWallpaperZoomDisabled) =
        rememberSecureSettingBooleanState(WALLPAPER_ZOOM_DISABLED_SETTING)
    val maxWallpaperScale =
        remember(context) { getSystemWallpaperMaxScale(context) }
    val wallpaperZoomTargetScale =
        if (isLockOnly || wallpaperZoomDisabled) 1f else maxWallpaperScale
    val animatedWallpaperZoomScale by
        animateFloatAsState(
            targetValue = wallpaperZoomTargetScale,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "wallpaperZoomScale",
        )
    val wallpaperZoomScale =
        animatedWallpaperZoomScale.coerceIn(
            minOf(1f, maxWallpaperScale),
            maxOf(1f, maxWallpaperScale),
        )
    val currentWallpaperZoomScale by rememberUpdatedState(wallpaperZoomScale)

    val wallpaperDisplaySize = remember { DisplayUtils.getLargestInternalDisplaySize(context) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var primaryColors by remember { mutableStateOf<MonetPrimaryColors?>(null) }
    var originalWidth by remember { mutableIntStateOf(0) }
    var originalHeight by remember { mutableIntStateOf(0) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var minScale by remember { mutableFloatStateOf(1f) }
    var fitMode by remember { mutableStateOf(false) }
    var fitBgColor by remember { mutableStateOf(Color.Black) }

    var screenW by remember { mutableFloatStateOf(0f) }
    var screenH by remember { mutableFloatStateOf(0f) }

    var isLoading by remember { mutableStateOf(true) }
    var isApplying by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }

    var showHint by remember { mutableStateOf(true) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(imageUri, drawableRes, targetFlags, darkTheme) {
        isLoading = true
        loadError = false
        primaryColors = null
        try {
            val loaded =
                withContext(Dispatchers.IO) {
                    val dimensions =
                        if (imageUri != null && isStandaloneMode) {
                            getImageDimensions(context, imageUri)
                        } else {
                            null
                        }
                    val decoded =
                        when {
                            imageUri != null ->
                                decodeFromUri(context, imageUri, wallpaperDisplaySize)
                            drawableRes != 0 ->
                                getWallpaperDrawable(context, drawableRes)?.toBitmap()
                            else ->
                                getCurrentWallpaperBitmap(
                                    context,
                                    targetFlags != WallpaperManager.FLAG_LOCK,
                                )
                        } ?: return@withContext null

                    LoadedWallpaper(
                        bitmap = decoded,
                        dimensions = dimensions,
                        fitBackgroundColor =
                            Color(WallpaperColors.fromBitmap(decoded).primaryColor.toArgb()),
                        primaryColors = monetPrimaryColors(context, decoded, darkTheme),
                    )
                }

            if (loaded == null) {
                loadError = true
                return@LaunchedEffect
            }

            bitmap = loaded.bitmap
            originalWidth = loaded.dimensions?.first ?: loaded.bitmap.width
            originalHeight = loaded.dimensions?.second ?: loaded.bitmap.height
            fitBgColor = loaded.fitBackgroundColor
            primaryColors = loaded.primaryColors
            Log.d(TAG, "Decoded: ${loaded.bitmap.width}x${loaded.bitmap.height}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode image", e)
            loadError = true
        } finally {
            isLoading = false
        }
    }
    val colors = primaryColors ?: MaterialTheme.colorScheme

    LaunchedEffect(bitmap, screenW, screenH, fitMode) {
        val bmp = bitmap ?: return@LaunchedEffect
        if (screenW <= 0f || screenH <= 0f) return@LaunchedEffect

        if (fitMode) {
            minScale =
                min(screenW / bmp.width.toFloat(), screenH / bmp.height.toFloat())
        } else {
            minScale =
                calculateMinScale(screenW, screenH, bmp.width.toFloat(), bmp.height.toFloat())
        }
        scale = minScale
        offsetX = 0f
        offsetY = 0f
    }

    LaunchedEffect(wallpaperZoomTargetScale) {
        val bmp = bitmap ?: return@LaunchedEffect
        if (screenW <= 0f || screenH <= 0f) return@LaunchedEffect

        offsetX =
            clampOffset(
                offsetX,
                bmp.width * scale,
                screenW / wallpaperZoomTargetScale,
            )
        offsetY =
            clampOffset(
                offsetY,
                bmp.height * scale,
                screenH / wallpaperZoomTargetScale,
            )
    }

    LaunchedEffect(showHint) {
        if (showHint) {
            kotlinx.coroutines.delay(3000)
            showHint = false
        }
    }

    BackHandler { onCancel() }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawRect(Color.Black) }

        if (fitMode) {
            Box(modifier = Modifier.fillMaxSize().background(fitBgColor))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator(
                    modifier = Modifier.size(48.dp),
                    containerColor = colors.primaryContainer,
                    indicatorColor = colors.onPrimaryContainer,
                )
            }
        } else if (loadError) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.failed_to_load_image),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            val bmp = bitmap
            if (bmp != null) {
                val imgW = bmp.width.toFloat()
                val imgH = bmp.height.toFloat()

                Canvas(
                    modifier =
                        Modifier.fillMaxSize()
                            .then(
                                if (fitMode) Modifier
                                else
                                    Modifier.pointerInput(bmp) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            showHint = false
                                            val newScale =
                                                (scale * zoom).coerceIn(
                                                    minScale,
                                                    minScale * MAX_ZOOM_FACTOR,
                                                )
                                            val scaleChange = newScale / scale
                                            val newOffsetX =
                                                offsetX * scaleChange +
                                                    pan.x / currentWallpaperZoomScale
                                            val newOffsetY =
                                                offsetY * scaleChange +
                                                    pan.y / currentWallpaperZoomScale

                                            scale = newScale
                                            offsetX =
                                                clampOffset(
                                                    newOffsetX,
                                                    imgW * newScale,
                                                    screenW / currentWallpaperZoomScale,
                                                )
                                            offsetY =
                                                clampOffset(
                                                    newOffsetY,
                                                    imgH * newScale,
                                                    screenH / currentWallpaperZoomScale,
                                                )
                                        }
                                    }
                            )
                            .pointerInput(bmp) {
                                detectTapGestures(
                                    onDoubleTap = doubleTap@ { tapOffset ->
                                        if (fitMode) return@doubleTap
                                        if (scale > minScale * 1.1f) {
                                            scale = minScale
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            val targetScale =
                                                (minScale * 3f).coerceAtMost(
                                                    minScale * MAX_ZOOM_FACTOR
                                                )
                                            val focusX =
                                                (tapOffset.x - screenW / 2f) /
                                                    currentWallpaperZoomScale
                                            val focusY =
                                                (tapOffset.y - screenH / 2f) /
                                                    currentWallpaperZoomScale
                                            val scaleChange = targetScale / scale

                                            scale = targetScale
                                            offsetX =
                                                clampOffset(
                                                    (offsetX - focusX) * scaleChange + focusX,
                                                    imgW * targetScale,
                                                    screenW / currentWallpaperZoomScale,
                                                )
                                            offsetY =
                                                clampOffset(
                                                    (offsetY - focusY) * scaleChange + focusY,
                                                    imgH * targetScale,
                                                    screenH / currentWallpaperZoomScale,
                                                )
                                        }
                                    },
                                    onTap = {
                                        showHint = false
                                        controlsVisible = !controlsVisible
                                    },
                                )
                            }
                ) {
                    screenW = size.width
                    screenH = size.height

                    drawIntoCanvas { canvas ->
                        canvas.withSave {
                            canvas.translate(size.width / 2f, size.height / 2f)
                            canvas.scale(wallpaperZoomScale, wallpaperZoomScale)
                            canvas.translate(offsetX, offsetY)
                            canvas.scale(scale, scale)
                            canvas.translate(-imgW / 2f, -imgH / 2f)
                            canvas.nativeCanvas.drawBitmap(bmp, 0f, 0f, null)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible || isLoading || loadError,
            enter =
                fadeIn(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { -it },
            exit =
                fadeOut(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { -it },
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.preview_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                modifier = Modifier.statusBarsPadding(),
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !isLoading && !loadError,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter =
                fadeIn(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
            exit =
                fadeOut(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it },
        ) {
            MaterialTheme(colorScheme = colors) {
                Card(
                    modifier =
                        Modifier.navigationBarsPadding()
                            .padding(12.dp)
                            .widthIn(max = 520.dp)
                            .fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AnimatedVisibility(
                            visible = showHint && !fitMode,
                            enter =
                                fadeIn(
                                    animationSpec =
                                        MaterialTheme.motionScheme.fastEffectsSpec()
                                ),
                            exit =
                                fadeOut(
                                    animationSpec =
                                        MaterialTheme.motionScheme.fastEffectsSpec()
                                ),
                        ) {
                            Text(
                                text = stringResource(R.string.pinch_to_crop),
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }

                        if (showFitModeToggle) {
                            WallpaperModeSelector(
                                fitMode = fitMode,
                                onFitModeChange = { fitMode = it },
                            )
                        }

                        if (!isLockOnly) {
                            WallpaperZoomToggle(
                                enabled = !wallpaperZoomDisabled,
                                onEnabledChange = { setWallpaperZoomDisabled(!it) },
                            )
                        }

                        val actionButtonHeight = ButtonDefaults.MediumContainerHeight
                        Button(
                            onClick = {
                                if (isStandaloneMode) {
                                    showTargetDialog = true
                                } else {
                                    bitmap?.let { bmp ->
                                        if (fitMode) {
                                            val result = buildFitWp(bmp, wallpaperDisplaySize)
                                            onNext?.invoke(
                                                result,
                                                computeDisplayCropHints(
                                                    context,
                                                    result.width,
                                                    result.height,
                                                ),
                                            )
                                        } else {
                                            val cropRect =
                                                calculateCropRect(
                                                    screenW,
                                                    screenH,
                                                    bmp.width,
                                                    bmp.height,
                                                    originalWidth,
                                                    originalHeight,
                                                    scale,
                                                    offsetX,
                                                    offsetY,
                                                )
                                            val cropHints =
                                                buildMultiDisplayCropHints(
                                                    context = context,
                                                    wallpaperSize =
                                                        Point(originalWidth, originalHeight),
                                                    previewBitmapSize =
                                                        Point(bmp.width, bmp.height),
                                                    userCropRect = cropRect,
                                                    wallpaperZoom = scale,
                                                    hostViewSize =
                                                        Point(screenW.toInt(), screenH.toInt()),
                                                )
                                            val (rebasedBitmap, rebasedCropHints) =
                                                rebaseWallpaperToCropHints(bmp, cropHints)
                                            onNext?.invoke(
                                                rebasedBitmap,
                                                rebasedCropHints,
                                            )
                                        }
                                    }
                                }
                            },
                            shapes = ButtonDefaults.shapesFor(actionButtonHeight),
                            modifier =
                                Modifier.fillMaxWidth().heightIn(min = actionButtonHeight),
                            enabled = !isApplying && bitmap != null,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor =
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    disabledContentColor =
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                ),
                            contentPadding =
                                ButtonDefaults.contentPaddingFor(actionButtonHeight),
                        ) {
                            if (isApplying) {
                                LoadingIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(
                                    text =
                                        stringResource(
                                            if (isStandaloneMode) R.string.set_wallpaper
                                            else R.string.next_button
                                        ),
                                    style = ButtonDefaults.textStyleFor(actionButtonHeight),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTargetDialog && imageUri != null) {
        WallpaperTargetDialog(
            colors = colors,
            onDismiss = { showTargetDialog = false },
            onSelect = { flags ->
                showTargetDialog = false
                isApplying = true

                val bmp = bitmap ?: return@WallpaperTargetDialog

                if (fitMode) {
                    val composite = buildFitWp(bmp, wallpaperDisplaySize)
                    Log.d(
                        TAG,
                        "Apply (fit): flags=$flags, " +
                            "composite=${composite.width}x${composite.height}",
                    )
                    onApplyBitmap?.invoke(composite, flags)
                    return@WallpaperTargetDialog
                }

                val cropRect =
                    calculateCropRect(
                        screenW,
                        screenH,
                        bmp.width,
                        bmp.height,
                        originalWidth,
                        originalHeight,
                        scale,
                        offsetX,
                        offsetY,
                    )

                val multiCropHints =
                    buildMultiDisplayCropHints(
                        context = context,
                        wallpaperSize = Point(originalWidth, originalHeight),
                        previewBitmapSize = Point(bmp.width, bmp.height),
                        userCropRect = cropRect,
                        wallpaperZoom = scale,
                        hostViewSize = Point(screenW.toInt(), screenH.toInt()),
                    )

                Log.d(
                    TAG,
                    "Apply: flags=$flags, cropRect=$cropRect, " +
                        "multiCrop=${multiCropHints.size} displays",
                )
                onApply?.invoke(imageUri, cropRect, multiCropHints, flags)
            },
        )
    }
}

@Composable
private fun WallpaperModeSelector(
    fitMode: Boolean,
    onFitModeChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val buttonHeight = ButtonDefaults.MediumContainerHeight
    val fillInteraction = remember { MutableInteractionSource() }
    val fitInteraction = remember { MutableInteractionSource() }
    val buttonColors =
        ToggleButtonDefaults.toggleButtonColors(
            checkedContainerColor = colors.secondaryContainer,
            checkedContentColor = colors.onSecondaryContainer,
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurfaceVariant,
        )

    ButtonGroup(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToggleButton(
            checked = !fitMode,
            onCheckedChange = { onFitModeChange(false) },
            modifier =
                Modifier.weight(1f)
                    .animateWidth(fillInteraction)
                    .heightIn(min = buttonHeight),
            shapes = ToggleButtonDefaults.shapesFor(buttonHeight),
            colors = buttonColors,
            contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
            interactionSource = fillInteraction,
        ) {
            Icon(
                imageVector = Icons.Default.CropFree,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(buttonHeight)))
            Text(
                text = stringResource(R.string.wallpaper_fill_mode),
                style = ButtonDefaults.textStyleFor(buttonHeight),
                fontWeight = FontWeight.SemiBold,
            )
        }

        ToggleButton(
            checked = fitMode,
            onCheckedChange = { onFitModeChange(true) },
            modifier =
                Modifier.weight(1f)
                    .animateWidth(fitInteraction)
                    .heightIn(min = buttonHeight),
            shapes = ToggleButtonDefaults.shapesFor(buttonHeight),
            colors = buttonColors,
            contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
            interactionSource = fitInteraction,
        ) {
            Icon(
                imageVector = Icons.Default.FitScreen,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(buttonHeight)))
            Text(
                text = stringResource(R.string.wallpaper_fit_mode),
                style = ButtonDefaults.textStyleFor(buttonHeight),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun WallpaperZoomToggle(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .toggleable(
                        value = enabled,
                        role = Role.Switch,
                        onValueChange = onEnabledChange,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.wallpaper_zoom_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.wallpaper_zoom_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}

private fun buildFitWp(src: Bitmap, target: Point): Bitmap {
    val targetW = target.x.coerceAtLeast(1)
    val targetH = target.y.coerceAtLeast(1)
    val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    val colors = WallpaperColors.fromBitmap(src)
    canvas.drawColor(colors.primaryColor.toArgb())

    val fgScale =
        min(targetW.toFloat() / src.width, targetH.toFloat() / src.height)
    val fgW = (src.width * fgScale).roundToInt().coerceAtLeast(1)
    val fgH = (src.height * fgScale).roundToInt().coerceAtLeast(1)
    val fgLeft = (targetW - fgW) / 2f
    val fgTop = (targetH - fgH) / 2f
    val matrix =
        Matrix().apply {
            postScale(fgScale, fgScale)
            postTranslate(fgLeft, fgTop)
        }
    canvas.drawBitmap(src, matrix, paint)
    return out
}

private fun decodeFromUri(context: Context, uri: Uri, wallpaperDisplaySize: Point): Bitmap? {
    val targetW = wallpaperDisplaySize.x * 2
    val targetH = wallpaperDisplaySize.y * 2

    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }

    var sampleSize = 1
    while (options.outWidth / sampleSize > targetW && options.outHeight / sampleSize > targetH) {
        sampleSize *= 2
    }

    val decodeOptions =
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    }
}
