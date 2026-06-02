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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.android.axion.themepicker.ui.sections

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.android.axion.themepicker.R
import com.android.axion.themepicker.data.model.WallpaperInfo
import com.android.axion.themepicker.ui.components.SectionHeader
import com.android.axion.themepicker.ui.theme.LocalAdaptiveLayoutInfo
import com.android.axion.themepicker.ui.theme.LocalExpressiveDesign
import com.android.axion.themepicker.utils.wallpaper.getCurrentWallpaperBitmap
import com.android.axion.themepicker.utils.wallpaper.getWallpaperDrawable
import com.android.axion.themepicker.utils.wallpaper.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WallpaperSection(
    wallpapers: List<WallpaperInfo>,
    onWallpaperSelected: (WallpaperInfo) -> Unit,
    onEditCurrent: () -> Unit,
    onOpenGallery: () -> Unit,
    onSelectPhoto: () -> Unit,
    onOpenEffects: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val design = LocalExpressiveDesign.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    var isProcessing by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        isProcessing = false
        onPauseOrDispose {}
    }

    if (isProcessing) {
        LoadingOverlayDialog(onDismissRequest = {})
    }

    val guardedSelectPhoto: () -> Unit = {
        if (!isProcessing) {
            isProcessing = true
            onSelectPhoto()
        }
    }

    val guardedWallpaperSelected: (WallpaperInfo) -> Unit = { wallpaper ->
        if (!isProcessing) {
            isProcessing = true
            onWallpaperSelected(wallpaper)
        }
    }

    val wallpaperBitmap by
        produceState<ImageBitmap?>(null) {
            value =
                withContext(Dispatchers.IO) {
                    getCurrentWallpaperBitmap(context, true)?.asImageBitmap()
                }
        }

    if (layoutInfo.isDualPane) {
        Row(
            modifier = modifier.fillMaxSize().padding(design.spacing.screenPaddingTablet),
            horizontalArrangement = Arrangement.spacedBy(design.spacing.large),
        ) {
            HeroCard(
                wallpaperBitmap = wallpaperBitmap,
                onEditCurrent = onEditCurrent,
                onOpenGallery = onOpenGallery,
                onSelectPhoto = guardedSelectPhoto,
                onOpenEffects = onOpenEffects,
                modifier = Modifier.weight(0.5f).fillMaxHeight(),
            )

            Column(
                modifier = Modifier.weight(0.5f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(design.spacing.medium),
            ) {
                SectionHeader(
                    title = stringResource(R.string.featured),
                    subtitle = stringResource(R.string.curated_wallpapers_for_you),
                    actionLabel = stringResource(R.string.explore),
                    onAction = onOpenGallery,
                )
                FeaturedCarousel(
                    wallpapers = wallpapers,
                    onWallpaperSelected = guardedWallpaperSelected,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(design.spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(design.spacing.large),
        ) {
            item {
                HeroCard(
                    wallpaperBitmap = wallpaperBitmap,
                    onEditCurrent = onEditCurrent,
                    onOpenGallery = onOpenGallery,
                    onSelectPhoto = guardedSelectPhoto,
                    onOpenEffects = onOpenEffects,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.explore),
                    subtitle = stringResource(R.string.discover_new_wallpapers),
                    actionLabel = stringResource(R.string.explore),
                    onAction = onOpenGallery,
                )
            }

            item {
                FeaturedCarousel(
                    wallpapers = wallpapers.take(10),
                    onWallpaperSelected = guardedWallpaperSelected,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    wallpaperBitmap: ImageBitmap?,
    onEditCurrent: () -> Unit,
    onOpenGallery: () -> Unit,
    onSelectPhoto: () -> Unit,
    onOpenEffects: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val design = LocalExpressiveDesign.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(design.shapes.cardCorner),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            wallpaperBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier =
                        Modifier.fillMaxSize()
                            .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                    contentScale = ContentScale.Crop,
                )
            }

            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Black.copy(alpha = 0.7f),
                                        )
                                )
                        )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(design.spacing.large),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.primary.copy(alpha = 0.9f),
                        contentColor = colors.onPrimary,
                    ) {
                        Text(
                            text = stringResource(R.string.your_wallpaper),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.make_it_yours),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )

                    Text(
                        text = stringResource(R.string.customize_to_match_your_style),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    wallpaperBitmap?.let { sharpBitmap ->
                        Card(
                            modifier = Modifier.fillMaxHeight().aspectRatio(0.55f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        ) {
                            Image(
                                bitmap = sharpBitmap,
                                contentDescription = stringResource(R.string.current_wallpaper),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }

                val configuration = LocalContext.current.resources.configuration
                val screenWidthDp = configuration.screenWidthDp
                val showLabels = screenWidthDp >= 360

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (showLabels) Arrangement.spacedBy(8.dp) else Arrangement.SpaceEvenly,
                ) {
                    WallpaperActionChip(
                        title = stringResource(R.string.photos),
                        icon = Icons.Outlined.Photo,
                        onClick = onSelectPhoto,
                        showLabel = showLabels,
                        modifier = if (showLabels) Modifier.weight(1f) else Modifier,
                    )
                    WallpaperActionChip(
                        title = stringResource(R.string.edit),
                        icon = Icons.Outlined.Tune,
                        onClick = onEditCurrent,
                        showLabel = showLabels,
                        modifier = if (showLabels) Modifier.weight(1f) else Modifier,
                    )
                    WallpaperActionChip(
                        title = stringResource(R.string.wallpaper_effects_title),
                        icon = Icons.Outlined.AutoAwesome,
                        onClick = onOpenEffects,
                        showLabel = showLabels,
                        modifier = if (showLabels) Modifier.weight(1f) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperActionChip(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by
        animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "action_chip_scale",
        )

    Surface(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        shape = if (showLabel) RoundedCornerShape(20.dp) else CircleShape,
        color = Color.White.copy(alpha = 0.2f),
    ) {
        if (showLabel) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }
        } else {
            Box(
                modifier = Modifier.size(48.dp).padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeaturedCarousel(
    wallpapers: List<WallpaperInfo>,
    onWallpaperSelected: (WallpaperInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state = rememberCarouselState { wallpapers.size }
    val snapSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val carouselShape = MaterialTheme.shapes.large

    HorizontalMultiBrowseCarousel(
        state = state,
        modifier = modifier.fillMaxWidth().height(220.dp),
        preferredItemWidth = 140.dp,
        itemSpacing = 8.dp,
        flingBehavior =
            CarouselDefaults.multiBrowseFlingBehavior(state = state, snapAnimationSpec = snapSpec),
    ) { i ->
        val wallpaper = wallpapers[i]
        val drawable =
            remember(wallpaper.drawableRes) { getWallpaperDrawable(context, wallpaper.drawableRes) }

        if (drawable != null) {
            val painter = rememberDrawablePainter(drawable)
            Image(
                painter = painter,
                contentDescription = wallpaper.title,
                modifier =
                    Modifier.height(220.dp).maskClip(carouselShape).clickable {
                        onWallpaperSelected(wallpaper)
                    },
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier =
                    Modifier.height(220.dp)
                        .fillMaxWidth()
                        .maskClip(carouselShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingOverlayDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        icon = { ContainedLoadingIndicator(modifier = Modifier.size(48.dp)) },
        title = {
            Text(
                text = stringResource(R.string.loading),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}
