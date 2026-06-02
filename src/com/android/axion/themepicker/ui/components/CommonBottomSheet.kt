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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.themepicker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlin.coroutines.*
import kotlinx.coroutines.*

object SheetDimens {
    val SheetCorner = 28.dp
    val SheetTopPadding = 24.dp
    val SheetPagerTop = 40.dp
    val SheetPagerPadding = 24.dp
    val SheetPagerSpacing = 16.dp
    val SheetPagerSpacingNav = 28.dp
    val SheetCloseSize = 40.dp
    val SheetSpacerSmall = 12.dp
    val SheetSpacerMedium = 16.dp
}

@Composable
fun CommonBottomSheet(
    visible: Boolean,
    title: String,
    heightFraction: Float = 0.4f,
    surfaceColor: Color? = null,
    scrimAlpha: Float = 0.32f,
    onDismiss: () -> Unit,
    onOffsetChanged: ((currentOffset: Float, maxOffset: Float) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val sheetSpatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val sheetEffectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val sheetHeightPx = screenHeightPx * heightFraction

        val offsetY = remember { Animatable(sheetHeightPx + 50f) }
        var isDismissing by remember { mutableStateOf(false) }
        val currentOnDismiss by rememberUpdatedState(onDismiss)
        val currentOnOffsetChanged by rememberUpdatedState(onOffsetChanged)

        LaunchedEffect(sheetHeightPx) {
            snapshotFlow { offsetY.value }
                .collect { value -> currentOnOffsetChanged?.invoke(value, sheetHeightPx + 50f) }
        }

        LaunchedEffect(visible) {
            if (visible) {
                isDismissing = false
                offsetY.animateTo(targetValue = 0f, animationSpec = sheetSpatialSpec)
            } else {
                offsetY.animateTo(
                    targetValue = sheetHeightPx + 50f,
                    animationSpec = sheetSpatialSpec,
                )
            }
        }

        val backdropAlpha by
            animateFloatAsState(
                targetValue = if (visible) scrimAlpha else 0f,
                animationSpec = sheetEffectsSpec,
                label = "BackdropAlpha",
            )

        if (visible || offsetY.value < sheetHeightPx) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(colors.scrim.copy(alpha = backdropAlpha))
                        .pointerInput(sheetHeightPx) {
                            detectTapGestures(
                                onTap = {
                                    if (isDismissing) return@detectTapGestures
                                    isDismissing = true
                                    coroutineScope.launch {
                                        offsetY.animateTo(
                                            targetValue = sheetHeightPx + 50f,
                                            animationSpec = sheetSpatialSpec,
                                        )
                                        currentOnDismiss()
                                    }
                                }
                            )
                        },
                contentAlignment = Alignment.BottomCenter,
            ) {
                val sheetShape =
                    RoundedCornerShape(
                        topStart = SheetDimens.SheetCorner,
                        topEnd = SheetDimens.SheetCorner,
                    )
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(with(density) { sheetHeightPx.toDp() })
                            .graphicsLayer { translationY = offsetY.value }
                            .background(surfaceColor ?: colors.surface, sheetShape)
                            .clip(sheetShape)
                            .pointerInput(sheetHeightPx) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        val newOffset =
                                            (offsetY.value + dragAmount).coerceAtLeast(0f)
                                        coroutineScope.launch { offsetY.snapTo(newOffset) }
                                    },
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            if (offsetY.value > sheetHeightPx * 0.25f) {
                                                offsetY.animateTo(
                                                    targetValue = sheetHeightPx + 50f,
                                                    animationSpec = sheetSpatialSpec,
                                                )
                                                currentOnDismiss()
                                            } else {
                                                offsetY.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = sheetSpatialSpec,
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = SheetDimens.SheetTopPadding)
                    ) {
                        Box(
                            modifier =
                                Modifier.width(32.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.outlineVariant)
                                    .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface,
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(horizontal = SheetDimens.SheetPagerPadding)
                                    .padding(bottom = 20.dp),
                            textAlign = TextAlign.Start,
                        )

                        content()
                    }
                }
            }
        }
    }
}
