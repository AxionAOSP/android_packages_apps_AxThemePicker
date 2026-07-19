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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DualDisplayPreview(
    wallpaperBitmap: Bitmap?,
    foldedDisplaySize: Point,
    unfoldedDisplaySize: Point,
    wallpaperZoomScale: Float,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    spacing: Dp = 8.dp,
) {
    val foldedAR = foldedDisplaySize.x.toFloat() / foldedDisplaySize.y
    val unfoldedAR = unfoldedDisplaySize.x.toFloat() / unfoldedDisplaySize.y

    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        DisplayPreviewCard(
            bitmap = wallpaperBitmap,
            aspectRatio = unfoldedAR,
            cornerRadius = cornerRadius,
            wallpaperZoomScale = wallpaperZoomScale,
            modifier = Modifier.weight(unfoldedAR),
        )

        DisplayPreviewCard(
            bitmap = wallpaperBitmap,
            aspectRatio = foldedAR,
            cornerRadius = cornerRadius,
            wallpaperZoomScale = wallpaperZoomScale,
            modifier = Modifier.weight(foldedAR),
        )
    }
}

@Composable
private fun DisplayPreviewCard(
    bitmap: Bitmap?,
    aspectRatio: Float,
    cornerRadius: Dp,
    wallpaperZoomScale: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(cornerRadius)

    Card(
        modifier = modifier.aspectRatio(aspectRatio),
        shape = shape,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        bitmap?.let { bmp ->
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
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
