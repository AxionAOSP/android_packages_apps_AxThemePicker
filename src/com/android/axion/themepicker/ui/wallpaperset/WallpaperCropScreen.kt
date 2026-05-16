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
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.themepicker.R
import com.android.axion.themepicker.utils.wallpaper.DisplayHelper
import com.android.axion.themepicker.utils.wallpaper.getCurrentWallpaperBitmap
import com.android.axion.themepicker.utils.wallpaper.getWallpaperDrawable
import com.google.android.renderscript.Toolkit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "WallpaperCropScreen"
private const val MAX_ZOOM_FACTOR = 8f

@Composable
fun WallpaperCropScreen(
    imageUri: Uri? = null,
    drawableRes: Int = 0,
    onNext: ((Bitmap) -> Unit)? = null,
    onApply: ((Uri, Rect, Map<Point, Rect>?, Int) -> Unit)? = null,
    onApplyBitmap: ((Bitmap, Int) -> Unit)? = null,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val isStandaloneMode = onApply != null || onApplyBitmap != null
    val showFitModeToggle = isStandaloneMode || onNext != null

    val wallpaperDisplaySize = remember { DisplayHelper.getWallpaperDisplaySize(context) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
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

    LaunchedEffect(imageUri, drawableRes) {
        isLoading = true
        loadError = false
        withContext(Dispatchers.IO) {
            try {
                val decoded: Bitmap? =
                    when {
                        imageUri != null -> {

                            if (isStandaloneMode) {
                                val dims = getImageDimensions(context, imageUri)
                                if (dims != null) {
                                    originalWidth = dims.first
                                    originalHeight = dims.second
                                }
                            }
                            decodeFromUri(context, imageUri, wallpaperDisplaySize)
                        }
                        drawableRes != 0 -> {
                            getWallpaperDrawable(context, drawableRes)?.toBitmap()
                        }
                        else -> getCurrentWallpaperBitmap(context, true)
                    }

                if (decoded == null) {
                    loadError = true
                    isLoading = false
                    return@withContext
                }

                bitmap = decoded
                Log.d(TAG, "Decoded: ${decoded.width}x${decoded.height}")

                fitBgColor = Color(WallpaperColors.fromBitmap(decoded).primaryColor.toArgb())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode image", e)
                loadError = true
            }
            isLoading = false
        }
    }

    LaunchedEffect(bitmap, screenW, screenH, fitMode) {
        val bmp = bitmap ?: return@LaunchedEffect
        if (screenW <= 0f || screenH <= 0f) return@LaunchedEffect

        if (fitMode) {
            minScale =
                min(screenW / bmp.width.toFloat(), screenH / bmp.height.toFloat())
        } else {
            val ms = calculateMinScale(screenW, screenH, bmp.width.toFloat(), bmp.height.toFloat())
            minScale =
                if (isStandaloneMode) {
                    val msForWallpaperDisplay =
                        calculateMinScale(
                            wallpaperDisplaySize.x.toFloat(),
                            wallpaperDisplaySize.y.toFloat(),
                            bmp.width.toFloat(),
                            bmp.height.toFloat(),
                        )
                    maxOf(ms, msForWallpaperDisplay)
                } else {
                    ms
                }
        }
        scale = minScale
        offsetX = 0f
        offsetY = 0f
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
                CircularProgressIndicator(color = Color.White)
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
                                            val newOffsetX = offsetX * scaleChange + pan.x
                                            val newOffsetY = offsetY * scaleChange + pan.y

                                            scale = newScale
                                            offsetX =
                                                clampOffset(newOffsetX, imgW * newScale, screenW)
                                            offsetY =
                                                clampOffset(newOffsetY, imgH * newScale, screenH)
                                        }
                                    }
                            )
                            .then(
                                if (fitMode) Modifier
                                else
                                    Modifier.pointerInput(bmp) {
                                        detectTapGestures(
                                            onDoubleTap = { tapOffset ->
                                                if (scale > minScale * 1.1f) {
                                                    scale = minScale
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                } else {
                                                    val targetScale =
                                                        (minScale * 3f).coerceAtMost(
                                                            minScale * MAX_ZOOM_FACTOR
                                                        )
                                                    val focusX = tapOffset.x - screenW / 2f
                                                    val focusY = tapOffset.y - screenH / 2f
                                                    val scaleChange = targetScale / scale

                                                    scale = targetScale
                                                    offsetX =
                                                        clampOffset(
                                                            (offsetX - focusX) * scaleChange +
                                                                focusX,
                                                            imgW * targetScale,
                                                            screenW,
                                                        )
                                                    offsetY =
                                                        clampOffset(
                                                            (offsetY - focusY) * scaleChange +
                                                                focusY,
                                                            imgH * targetScale,
                                                            screenH,
                                                        )
                                                }
                                            }
                                        )
                                    }
                            )
                ) {
                    screenW = size.width
                    screenH = size.height

                    drawIntoCanvas { canvas ->
                        canvas.withSave {
                            canvas.translate(size.width / 2f + offsetX, size.height / 2f + offsetY)
                            canvas.scale(scale, scale)
                            canvas.translate(-imgW / 2f, -imgH / 2f)
                            canvas.nativeCanvas.drawBitmap(bmp, 0f, 0f, null)
                        }
                    }
                }
            }
        }

        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.statusBarsPadding(),
        )

        Column(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = showHint && !isLoading && !loadError && !fitMode,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = stringResource(R.string.pinch_to_crop),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            if (showFitModeToggle && !isLoading && !loadError) {
                Surface(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceBright,
                ) {
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ToggleButton(
                            checked = !fitMode,
                            onCheckedChange = { fitMode = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shapes = ToggleButtonDefaults.shapesFor(100.dp),
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.wallpaper_fill_mode),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        ToggleButton(
                            checked = fitMode,
                            onCheckedChange = { fitMode = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shapes = ToggleButtonDefaults.shapesFor(100.dp),
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.wallpaper_fit_mode),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (isStandaloneMode) {
                        showTargetDialog = true
                    } else {
                        bitmap?.let { bmp ->
                            val result =
                                if (fitMode) buildFitWp(bmp, wallpaperDisplaySize)
                                else extractVisibleBitmap(bmp, screenW, screenH, scale, offsetX, offsetY)
                            onNext?.invoke(result)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isApplying && bitmap != null,
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
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
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                    )
                }
            }
        }
    }

    if (showTargetDialog && imageUri != null) {
        WallpaperTargetDialog(
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
                        userCropRect = cropRect,
                        wallpaperZoom = scale,
                        hostViewSize = Point(screenW.toInt(), screenH.toInt()),
                    )

                Log.d(
                    TAG,
                    "Apply: flags=$flags, cropRect=$cropRect, " +
                        "multiCrop=${multiCropHints?.size ?: 0} displays",
                )
                onApply?.invoke(imageUri, cropRect, multiCropHints, flags)
            },
        )
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

@Composable
private fun WallpaperTargetDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_wallpaper_on)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onSelect(WallpaperManager.FLAG_SYSTEM) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.home_screen),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(
                    onClick = { onSelect(WallpaperManager.FLAG_LOCK) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.lock_screen_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(
                    onClick = {
                        onSelect(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.home_lock_both),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
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

private fun extractVisibleBitmap(
    sourceBitmap: Bitmap,
    screenW: Float,
    screenH: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): Bitmap {
    val imgW = sourceBitmap.width.toFloat()
    val imgH = sourceBitmap.height.toFloat()

    val visibleW = (screenW / scale).coerceAtMost(imgW)
    val visibleH = (screenH / scale).coerceAtMost(imgH)

    val centerX = imgW / 2f - offsetX / scale
    val centerY = imgH / 2f - offsetY / scale

    val left = (centerX - visibleW / 2f).coerceIn(0f, (imgW - visibleW).coerceAtLeast(0f))
    val top = (centerY - visibleH / 2f).coerceIn(0f, (imgH - visibleH).coerceAtLeast(0f))

    val w = visibleW.toInt().coerceIn(1, sourceBitmap.width - left.toInt())
    val h = visibleH.toInt().coerceIn(1, sourceBitmap.height - top.toInt())

    return Bitmap.createBitmap(sourceBitmap, left.toInt(), top.toInt(), w, h)
}
