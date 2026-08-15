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

package com.android.axion.themepicker.ui.wallpaperset

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.View
import com.android.axion.util.DisplayUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        val w = options.outWidth
        val h = options.outHeight
        if (w > 0 && h > 0) Pair(w, h) else null
    } catch (e: Exception) {
        null
    }
}

fun calculateMinScale(screenW: Float, screenH: Float, imgW: Float, imgH: Float): Float =
    maxOf(screenW / imgW, screenH / imgH)

fun clampOffset(offset: Float, scaledDim: Float, screenDim: Float): Float {
    val maxOffset = ((scaledDim - screenDim) / 2f).coerceAtLeast(0f)
    return offset.coerceIn(-maxOffset, maxOffset)
}

fun calculateCropRect(
    screenW: Float,
    screenH: Float,
    displayBmpW: Int,
    displayBmpH: Int,
    originalW: Int,
    originalH: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): Rect {

    val centerX = displayBmpW / 2f - offsetX / scale
    val centerY = displayBmpH / 2f - offsetY / scale

    val visibleW = screenW / scale
    val visibleH = screenH / scale

    val scaleX = originalW.toFloat() / displayBmpW
    val scaleY = originalH.toFloat() / displayBmpH

    val left = ((centerX - visibleW / 2f) * scaleX).roundToInt().coerceAtLeast(0)
    val top = ((centerY - visibleH / 2f) * scaleY).roundToInt().coerceAtLeast(0)
    val right = ((centerX + visibleW / 2f) * scaleX).roundToInt().coerceAtMost(originalW)
    val bottom = ((centerY + visibleH / 2f) * scaleY).roundToInt().coerceAtMost(originalH)

    return Rect(left, top, right, bottom)
}

fun wallpaperTravelToScreenWidthRatio(width: Int, height: Int): Float {
    val aspectRatio = width / height.toFloat()
    val x = (1.2f - 1.5f) / (16f / 10f - 10f / 16f)
    val y = 1.5f - x * (10f / 16f)
    return x * aspectRatio + y
}

fun getDefaultCropSurfaceSize(resources: Resources, screenW: Int, screenH: Int): Point {
    val maxDim = max(screenW, screenH)
    val minDim = minOf(screenW, screenH)
    val defaultWidth =
        if (resources.configuration.smallestScreenWidthDp >= 720) {
            (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim)).toInt()
        } else {
            max(minDim * 2, maxDim)
        }
    val defaultHeight = if (screenW < screenH) maxDim else minDim
    return Point(defaultWidth, defaultHeight)
}

fun getDefaultCropSurfaceSize(resources: Resources, displaySize: Point): Point {
    return getDefaultCropSurfaceSize(resources, displaySize.x, displaySize.y)
}

private const val CROP_TAG = "CropUtils"

fun calculateMinZoom(outer: Point, inner: Point): Float {
    return if (inner.x.toFloat() / inner.y > outer.x.toFloat() / outer.y) {
        inner.x.toFloat() / outer.x
    } else {
        inner.y.toFloat() / outer.y
    }
}

fun calculateVisibleRect(outer: Point, inner: Point): Rect {
    val center = PointF(outer.x / 2f, outer.y / 2f)
    return if (inner.x.toFloat() / inner.y > outer.x.toFloat() / outer.y) {
        val minZoom = inner.x.toFloat() / outer.x
        val visibleH = inner.y / minZoom
        Rect(0, (center.y - visibleH / 2).toInt(), outer.x, (center.y + visibleH / 2).toInt())
    } else {
        val minZoom = inner.y.toFloat() / outer.y
        val visibleW = inner.x / minZoom
        Rect((center.x - visibleW / 2).toInt(), 0, (center.x + visibleW / 2).toInt(), outer.y)
    }
}

fun calculateCropRectForDisplay(
    context: Context,
    wallpaperZoom: Float,
    wallpaperSize: Point,
    cropSurfaceSize: Point,
    targetHostSize: Point,
    scrollX: Int,
    scrollY: Int,
    cropExtraWidth: Boolean = true,
): Rect {
    val scaledW = (wallpaperSize.x * wallpaperZoom).roundToInt()
    val scaledH = (wallpaperSize.y * wallpaperZoom).roundToInt()
    val scaledWallpaperRect = Rect(0, 0, scaledW, scaledH)

    val cropRect = Rect(scrollX, scrollY, scrollX + targetHostSize.x, scrollY + targetHostSize.y)

    val extraWidth = cropSurfaceSize.x - targetHostSize.x
    val extraHeightTopAndBottom = ((cropSurfaceSize.y - targetHostSize.y) / 2f).toInt()

    if (cropExtraWidth) {
        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        if (isRtl) {
            cropRect.left = max(cropRect.left - extraWidth, scaledWallpaperRect.left)
        } else {
            cropRect.right = min(cropRect.right + extraWidth, scaledWallpaperRect.right)
        }
    }

    val availableExtraTop =
        max(cropRect.top - max(scaledWallpaperRect.top, cropRect.top - extraHeightTopAndBottom), 0)
    val availableExtraBottom =
        max(
            min(scaledWallpaperRect.bottom, cropRect.bottom + extraHeightTopAndBottom) -
                cropRect.bottom,
            0,
        )
    val extraHeightSymmetric = min(availableExtraTop, availableExtraBottom)
    cropRect.top -= extraHeightSymmetric
    cropRect.bottom += extraHeightSymmetric

    return cropRect
}

fun buildMultiDisplayCropHints(
    context: Context,
    wallpaperSize: Point,
    previewBitmapSize: Point,
    userCropRect: Rect,
    wallpaperZoom: Float,
    hostViewSize: Point,
): Map<Point, Rect> {
    val result = mutableMapOf<Point, Rect>()
    val centerX = userCropRect.exactCenterX()
    val centerY = userCropRect.exactCenterY()
    val previewMinZoom =
        calculateMinZoom(previewBitmapSize, hostViewSize).coerceAtLeast(Float.MIN_VALUE)
    val relativeZoom = (wallpaperZoom / previewMinZoom).coerceAtLeast(1f)

    val internalDisplays = DisplayUtils.getInternalDisplays(context)
    for (display in internalDisplays) {
        val size = DisplayUtils.getRealSize(display)
        val resources = context.createDisplayContext(display).resources

        for (displaySize in listOf(size, Point(size.y, size.x))) {
            val targetZoom = calculateMinZoom(wallpaperSize, displaySize) * relativeZoom
            val visibleWidth = (displaySize.x / targetZoom).coerceAtMost(wallpaperSize.x.toFloat())
            val visibleHeight = (displaySize.y / targetZoom).coerceAtMost(wallpaperSize.y.toFloat())
            val left =
                (centerX - visibleWidth / 2f)
                    .coerceIn(0f, wallpaperSize.x - visibleWidth)
            val top =
                (centerY - visibleHeight / 2f)
                    .coerceIn(0f, wallpaperSize.y - visibleHeight)
            val cropRect =
                calculateCropRectForDisplay(
                    context = context,
                    wallpaperZoom = targetZoom,
                    wallpaperSize = wallpaperSize,
                    cropSurfaceSize = getDefaultCropSurfaceSize(resources, displaySize),
                    targetHostSize = displaySize,
                    scrollX = (left * targetZoom).roundToInt(),
                    scrollY = (top * targetZoom).roundToInt(),
                )

            result[displaySize] =
                Rect(
                    (cropRect.left / targetZoom).roundToInt().coerceAtLeast(0),
                    (cropRect.top / targetZoom).roundToInt().coerceAtLeast(0),
                    (cropRect.right / targetZoom).roundToInt().coerceAtMost(wallpaperSize.x),
                    (cropRect.bottom / targetZoom).roundToInt().coerceAtMost(wallpaperSize.y),
                )
        }
    }

    Log.d(CROP_TAG, "Built multi-display crop hints for ${result.size} display configs")
    return result
}

internal fun rebaseWallpaperToCropHints(
    bitmap: Bitmap,
    cropHints: Map<Point, Rect>,
): Pair<Bitmap, Map<Point, Rect>> {
    val hints = cropHints.values.iterator()
    val bounds = Rect(hints.next())
    while (hints.hasNext()) {
        bounds.union(hints.next())
    }
    if (bounds == Rect(0, 0, bitmap.width, bitmap.height)) {
        return bitmap to cropHints
    }

    val croppedBitmap =
        Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
    val rebasedHints =
        cropHints.mapValues { (_, hint) ->
            Rect(hint).apply { offset(-bounds.left, -bounds.top) }
        }
    return croppedBitmap to rebasedHints
}

fun computeDisplayCropHints(
    context: Context,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Map<Point, Rect> {
    val displaySizes = DisplayUtils.getInternalDisplaySizes(context, true)
    val result = mutableMapOf<Point, Rect>()

    for (displaySize in displaySizes) {
        result[displaySize] =
            centerCropRect(bitmapWidth, bitmapHeight, displaySize.x, displaySize.y)
    }

    Log.d(
        CROP_TAG,
        "Computed center-crop hints for ${result.size} display configs " +
            "(bitmap=${bitmapWidth}x${bitmapHeight})",
    )
    return result
}

fun centerCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
    val srcAR = srcW.toFloat() / srcH
    val dstAR = dstW.toFloat() / dstH

    return if (srcAR > dstAR) {

        val cropW = (srcH * dstAR).roundToInt().coerceAtMost(srcW)
        val left = (srcW - cropW) / 2
        Rect(left, 0, left + cropW, srcH)
    } else {

        val cropH = (srcW / dstAR).roundToInt().coerceAtMost(srcH)
        val top = (srcH - cropH) / 2
        Rect(0, top, srcW, top + cropH)
    }
}
