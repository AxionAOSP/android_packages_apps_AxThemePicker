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

package com.android.axion.themepicker.ui.carousel

import android.os.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.axion.themepicker.R
import com.android.axion.themepicker.data.model.WallpaperInfo
import com.android.axion.themepicker.utils.math.sdp
import com.android.axion.themepicker.utils.wallpaper.getWallpaperDrawable
import com.android.axion.themepicker.utils.wallpaper.rememberDrawablePainter
import com.android.axion.themepicker.viewmodel.MainScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperCarouselCard(
    wallpapers: List<WallpaperInfo>,
    mainScreenViewModel: MainScreenViewModel = viewModel(),
) {
    val onMoreClick = { mainScreenViewModel.onOpenGallery() }

    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val wallpaperDrawables =
        remember(wallpapers) {
            wallpapers.map { wallpaper -> getWallpaperDrawable(context, wallpaper.drawableRes) }
        }

    Card(
        modifier =
            Modifier.fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 20.sdp, end = 20.sdp, bottom = 28.sdp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth().wrapContentHeight().padding(top = 16.sdp, bottom = 16.sdp)
        ) {
            HorizontalMultiBrowseCarousel(
                state = rememberCarouselState { wallpapers.size },
                modifier =
                    Modifier.fillMaxWidth()
                        .wrapContentHeight()
                        .padding(start = 16.sdp, end = 16.sdp),
                preferredItemWidth = 90.sdp,
                itemSpacing = 8.sdp,
            ) { i ->
                val wallpaper = wallpapers[i]
                val drawable = wallpaperDrawables[i]
                if (drawable != null) {
                    val painter = rememberDrawablePainter(drawable)
                    BoxWithConstraints {
                        Image(
                            painter = painter,
                            contentDescription = wallpaper.title,
                            modifier =
                                Modifier.height(156.sdp)
                                    .maskClip(RoundedCornerShape(16.sdp))
                                    .clickable {
                                        mainScreenViewModel.onOpenWallpaperCrop(
                                            drawableRes = wallpaper.drawableRes
                                        )
                                    },
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.sdp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier.size(24.sdp)
                            .clip(CircleShape)
                            .border(1.dp, colors.onSurface, CircleShape)
                            .clickable { onMoreClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More wallpapers",
                        tint = colors.onSurface,
                    )
                }

                TextButton(onClick = onMoreClick) {
                    Text(
                        text = stringResource(R.string.more_wallpapers),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = colors.onSurface,
                    )
                }
            }
        }
    }
}
