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

package com.android.axion.themepicker.ui.lockscreen

import android.app.WallpaperColors
import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SimpleLockscreenPreview(wallpaperBitmap: Bitmap? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val wallpaper = wallpaperBitmap ?: return
    val scale = context.previewScale

    val isRegionDark by
        produceState(true, wallpaper) {
            value =
                withContext(Dispatchers.Default) {
                    val colors = WallpaperColors.fromBitmap(wallpaper)
                    (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0
                }
        }

    var affordanceSelections by remember { mutableStateOf<List<AffordanceSelection>>(emptyList()) }
    var affordanceList by remember { mutableStateOf<List<AffordanceInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        launch {
            AffordanceRepository.observeSelections(context).collect { affordanceSelections = it }
        }
        launch { AffordanceRepository.observeAffordances(context).collect { affordanceList = it } }
    }

    val imageBitmap = remember(wallpaper) { wallpaper.asImageBitmap() }

    Box(
        modifier = modifier
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(top = Dimens.ClockTopPadding * scale * 1.5f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EditablePreviewClock(
                isPreview = true,
                isRegionDark = isRegionDark,
                editable = false,
                depthEffectEnabled = false,
            )
            Spacer(modifier = Modifier.weight(1f))
            AffordanceOverlay(
                isPreview = true,
                scale = scale,
                selections = affordanceSelections,
                affordances = affordanceList,
            )
            Spacer(modifier = Modifier.height(24.dp * scale))
        }
    }
}
