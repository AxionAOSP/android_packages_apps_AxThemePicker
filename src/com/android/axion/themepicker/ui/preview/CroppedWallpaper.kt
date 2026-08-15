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

package com.android.axion.themepicker.ui.preview

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.android.axion.themepicker.ui.wallpaperset.centerCropRect
import kotlin.math.roundToInt

@Composable
internal fun CroppedWallpaper(
    bitmap: Bitmap,
    cropHint: Rect?,
    displaySize: Point,
    wallpaperZoomScale: Float,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val layoutDirection = LocalLayoutDirection.current
    val visibleCrop =
        remember(bitmap, cropHint, displaySize, layoutDirection) {
            val crop =
                Rect(
                    cropHint
                        ?: centerCropRect(
                            bitmap.width,
                            bitmap.height,
                            displaySize.x,
                            displaySize.y,
                        )
                )
            val parallax =
                crop.width() - displaySize.x * crop.height() / displaySize.y
            if (layoutDirection == LayoutDirection.Rtl) {
                crop.left += parallax
            } else {
                crop.right -= parallax
            }
            crop.intersect(0, 0, bitmap.width, bitmap.height)
            crop
        }

    Canvas(
        modifier =
            modifier.graphicsLayer(
                scaleX = wallpaperZoomScale,
                scaleY = wallpaperZoomScale,
            )
    ) {
        drawImage(
            image = imageBitmap,
            srcOffset = IntOffset(visibleCrop.left, visibleCrop.top),
            srcSize =
                IntSize(
                    visibleCrop.width().coerceAtLeast(1),
                    visibleCrop.height().coerceAtLeast(1),
                ),
            dstSize =
                IntSize(
                    size.width.roundToInt().coerceAtLeast(1),
                    size.height.roundToInt().coerceAtLeast(1),
                ),
        )
    }
}
