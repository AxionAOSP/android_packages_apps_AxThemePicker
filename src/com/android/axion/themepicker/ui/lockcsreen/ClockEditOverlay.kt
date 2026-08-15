/*
 * Copyright (C) 2026 AxionOS
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

import android.content.Context
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.axion.themepicker.ui.lockscreen.widgets.observeTaps
import com.android.axion.themepicker.utils.math.scaleRatio
import com.android.systemui.shared.clocks.ClockEditScaleGeometry
import com.android.systemui.shared.clocks.ClockSettingsRepository
import com.android.systemui.shared.clocks.ClockTopPaddingRange
import com.android.systemui.shared.clocks.ClockWidgetGridMetrics
import com.android.systemui.shared.clocks.ClockWidgetLayoutState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditablePreviewClock(
    isPreview: Boolean,
    isRegionDark: Boolean = true,
    editable: Boolean,
    modifier: Modifier = Modifier,
    depthSourceBoundsProvider: (() -> RectF?)? = null,
    depthSourceScale: Float = 1f,
    onClick: (() -> Unit)? = null,
    applyFluidSize: Boolean = true,
    applyFluidTopPadding: Boolean = true,
    depthEffectEnabled: Boolean = true,
    clockAnimationTrigger: Int = 0,
    lockscreenWidgetLayoutState: ClockWidgetLayoutState = ClockWidgetLayoutState.Empty,
    resetTrigger: Int = 0,
    minimumTopPaddingDp: Float = CLOCK_EDIT_TOP_PADDING_MIN_DP,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val previewScale = if (isPreview) context.previewScale else context.scaleRatio

    LaunchedEffect(context, configuration) {
        ClockSettingsRepository.init(context)
    }

    val repositorySizeScale by ClockSettingsRepository.sizeScale.collectAsState()
    val repositoryHorizontalOffset by ClockSettingsRepository.horizontalOffsetDp.collectAsState()
    val repositoryTopPadding by ClockSettingsRepository.topPaddingDp.collectAsState()
    val storedSizeScale = if (applyFluidSize) repositorySizeScale else 1f
    val storedHorizontalOffset = if (applyFluidTopPadding) repositoryHorizontalOffset else 0f
    val storedTopPadding = if (applyFluidTopPadding) repositoryTopPadding else 0f
    val availableClockWidthDp =
        if (isPreview) configuration.screenWidthDp.toFloat()
        else configuration.screenWidthDp / previewScale
    val fallbackScaleGeometry = remember(availableClockWidthDp) {
        ClockEditScaleGeometry.default(
            availableWidthDp = availableClockWidthDp,
            requestedScale = storedSizeScale,
            scaleRange = ClockSettingsRepository.sizeScaleRange,
        )
    }
    var scaleGeometry by remember(availableClockWidthDp) {
        mutableStateOf(fallbackScaleGeometry)
    }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }

    val horizontalOffsetRange =
        remember(scaleGeometry, availableClockWidthDp) {
            ClockHorizontalOffsetRange.from(
                geometry = scaleGeometry,
                availableWidthDp = availableClockWidthDp,
            )
        }

    val topPaddingRange = remember(lockscreenWidgetLayoutState, minimumTopPaddingDp) {
        val reservedHeight =
            ClockWidgetGridMetrics(
                cellSizeDp = Dimens.WidgetCellSize.value,
                cellGapDp = Dimens.WidgetCellGap.value,
            ).reservedHeightDp(lockscreenWidgetLayoutState)
        ClockTopPaddingRange(
            min = minimumTopPaddingDp.coerceAtLeast(CLOCK_EDIT_TOP_PADDING_MIN_DP),
            baseMax = ClockSettingsRepository.TOP_PADDING_MAX_DP,
            reservedHeightDp = reservedHeight,
        )
    }
    var editModel by remember {
        mutableStateOf(
            ClockEditModel.from(
                storedSizeScale = storedSizeScale,
                storedHorizontalOffsetDp = storedHorizontalOffset,
                storedTopPaddingDp = storedTopPadding,
                scaleGeometry = scaleGeometry,
                horizontalOffsetRange = horizontalOffsetRange,
                topPaddingRange = topPaddingRange,
            )
        )
    }
    var handledResetTrigger by remember { mutableIntStateOf(resetTrigger) }

    LaunchedEffect(
        storedSizeScale,
        storedHorizontalOffset,
        storedTopPadding,
        scaleGeometry,
        horizontalOffsetRange,
        topPaddingRange,
    ) {
        editModel = editModel.sync(
            storedSizeScale = storedSizeScale,
            storedHorizontalOffsetDp = storedHorizontalOffset,
            storedTopPaddingDp = storedTopPadding,
            scaleGeometry = scaleGeometry,
            horizontalOffsetRange = horizontalOffsetRange,
            topPaddingRange = topPaddingRange,
        )
    }

    LaunchedEffect(resetTrigger) {
        if (resetTrigger == handledResetTrigger) return@LaunchedEffect
        handledResetTrigger = resetTrigger
        editModel = editModel.resetSize().resetPosition()
        withContext(Dispatchers.IO) {
            ClockSettingsRepository.writeSizeAndPosition(context, 1f, 0f, 0f)
        }
    }

    val topPadding = when {
        editable -> editModel.topPaddingDp
        applyFluidTopPadding -> topPaddingRange.clamp(storedTopPadding)
        else -> storedTopPadding
    }
    val topPaddingDp = (topPadding.coerceAtLeast(0f) * previewScale).dp
    val topOffsetDp = (topPadding.coerceAtMost(0f) * previewScale).dp
    val handlesVisible = editable && !isPreview
    val previewSizeScaleOverride = when {
        !applyFluidSize -> 1f
        handlesVisible -> editModel.sizeScaleOverride
        else -> null
    }
    val previewHorizontalOffsetOverride = when {
        !applyFluidTopPadding -> 0f
        handlesVisible -> editModel.horizontalOffsetDp
        else -> null
    }
    val depthEffectVisible =
        depthEffectEnabled &&
            (!handlesVisible || editModel.dragTarget == ClockEditDragTarget.None)
    val freezePreviewAlignment =
        handlesVisible && editModel.dragTarget != ClockEditDragTarget.None
    val freezeEditGeometry =
        handlesVisible && editModel.dragTarget == ClockEditDragTarget.Size
    val guideColor = Color.White.copy(alpha = 0.4f)
    val handleColor = Color.White.copy(alpha = 0.58f)

    fun endSizeDrag() {
        val sizeScale = editModel.sizeScaleOverride
        val horizontalOffset = editModel.horizontalOffsetDp
        val topPadding = editModel.topPaddingDp
        scope.launch(Dispatchers.IO) {
            ClockSettingsRepository.writeSizeAndPosition(
                context,
                sizeScale,
                horizontalOffset,
                topPadding,
            )
        }
        editModel = editModel.commitDrag()
    }

    fun cancelSizeDrag() {
        editModel = editModel.cancelDrag()
    }

    fun cancelPositionDrag() {
        editModel = editModel.cancelDrag()
    }

    fun endPositionDrag() {
        val snappedModel = editModel.snapHorizontalPosition()
        val horizontalOffset = snappedModel.horizontalOffsetDp
        val topPadding = snappedModel.topPaddingDp
        scope.launch(Dispatchers.IO) {
            ClockSettingsRepository.writePosition(context, horizontalOffset, topPadding)
        }
        editModel = snappedModel.commitDrag()
    }

    val latestEditModel by rememberUpdatedState(editModel)
    val latestOnClick by rememberUpdatedState(onClick)

    fun resizeCornerAt(
        offset: Offset,
        widthPx: Float,
        heightPx: Float,
    ): ClockResizeCorner? {
        val model = latestEditModel
        val handleSizePx = CLOCK_EDIT_HANDLE_SIZE_DP * density
        val handleOverflowPx = CLOCK_EDIT_HANDLE_OVERFLOW_DP * density
        val unscaledWidthDp = widthPx / density / previewScale
        val unscaledHeightDp = heightPx / density / previewScale
        val frameBounds = ClockEditFrameBounds.from(model.scaleGeometry, unscaledWidthDp)
        val frameStartPx =
            frameBounds.startOffsetDp(
                unscaledWidthDp,
                model.horizontalOffsetDp,
            ) * previewScale * density
        val frameEndPx =
            frameBounds.endOffsetDp(
                unscaledWidthDp,
                model.horizontalOffsetDp,
            ) * previewScale * density
        val frameTopPx = frameBounds.topOffsetDp(unscaledHeightDp) * previewScale * density
        val frameBottomPx = frameBounds.bottomOffsetDp(unscaledHeightDp) * previewScale * density
        return ClockResizeCorner.entries.firstOrNull { corner ->
            val handleStartPx =
                corner.handleX(frameStartPx, frameEndPx, handleSizePx, handleOverflowPx)
            val handleTopPx =
                corner.handleY(frameTopPx, frameBottomPx, handleSizePx, handleOverflowPx)
            offset.x in handleStartPx..(handleStartPx + handleSizePx) &&
                offset.y in handleTopPx..(handleTopPx + handleSizePx)
        }
    }

    Box(
        modifier =
            modifier.fillMaxWidth()
                .wrapContentHeight()
                .padding(top = topPaddingDp)
                .offset(y = topOffsetDp),
        contentAlignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .onSizeChanged { overlaySize = it }
                    .then(
                        when {
                            handlesVisible && onClick != null ->
                                Modifier.pointerInput(previewScale, density) {
                                    observeTaps(pass = PointerEventPass.Final) { offset ->
                                        val resizeCorner =
                                            resizeCornerAt(
                                                offset,
                                                size.width.toFloat(),
                                                size.height.toFloat(),
                                            )
                                        if (resizeCorner == null) {
                                            latestOnClick?.invoke()
                                        }
                                    }
                                }
                            onClick != null -> Modifier.clickable { onClick() }
                            else -> Modifier
                        }
                    )
                    .then(
                        if (handlesVisible) {
                            Modifier.pointerInput(previewScale, density) {
                                var positionDragActive = false
                                var resizeCorner: ClockResizeCorner? = null
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        resizeCorner =
                                            resizeCornerAt(
                                                offset,
                                                size.width.toFloat(),
                                                size.height.toFloat(),
                                            )
                                        positionDragActive = resizeCorner == null
                                        if (resizeCorner != null) {
                                            editModel = editModel.beginSizeDrag()
                                        } else {
                                            editModel = editModel.beginPositionDrag()
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount.x != 0f || dragAmount.y != 0f) {
                                            val corner = resizeCorner
                                            if (corner != null) {
                                                editModel =
                                                    editModel.resizeBy(
                                                        dragAmount.x,
                                                        dragAmount.y,
                                                        density,
                                                        previewScale,
                                                        corner,
                                                    )
                                            } else if (positionDragActive) {
                                                editModel =
                                                    editModel.movePositionBy(
                                                        dragAmount.x,
                                                        dragAmount.y,
                                                        density,
                                                        previewScale,
                                                    )
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (resizeCorner != null) {
                                            endSizeDrag()
                                        } else if (positionDragActive) {
                                            endPositionDrag()
                                        }
                                        resizeCorner = null
                                        positionDragActive = false
                                    },
                                    onDragCancel = {
                                        if (resizeCorner != null) {
                                            cancelSizeDrag()
                                        } else if (positionDragActive) {
                                            cancelPositionDrag()
                                        }
                                        resizeCorner = null
                                        positionDragActive = false
                                    },
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (handlesVisible) {
                            Modifier.drawBehind {
                                val unscaledWidthDp = size.width / density / previewScale
                                val unscaledHeightDp = size.height / density / previewScale
                                val frameBounds =
                                    ClockEditFrameBounds.from(
                                        editModel.scaleGeometry,
                                        unscaledWidthDp,
                                    )
                                val frameWidthPx =
                                    frameBounds.widthIn(unscaledWidthDp) * previewScale * density
                                val frameStartPx =
                                    frameBounds.startOffsetDp(
                                        unscaledWidthDp,
                                        editModel.horizontalOffsetDp,
                                    ) * previewScale * density
                                val frameTopPx =
                                    frameBounds.topOffsetDp(unscaledHeightDp) *
                                        previewScale *
                                        density
                                val strokeWidth = CLOCK_EDIT_GUIDE_STROKE_DP.dp.toPx()
                                val strokeInset = strokeWidth / 2f
                                val frameHeightPx =
                                    frameBounds.heightIn(unscaledHeightDp) * previewScale * density
                                drawRoundRect(
                                    color = guideColor,
                                    topLeft = Offset(
                                        frameStartPx + strokeInset,
                                        frameTopPx + strokeInset,
                                    ),
                                    size = Size(
                                        (frameWidthPx - strokeWidth).coerceAtLeast(0f),
                                        (frameHeightPx - strokeWidth).coerceAtLeast(0f),
                                    ),
                                    cornerRadius = CornerRadius(
                                        CLOCK_EDIT_GUIDE_CORNER_RADIUS_DP.dp.toPx(),
                                    ),
                                    style = Stroke(strokeWidth),
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
        ) {
            val unscaledWidthDp = maxWidth.value / previewScale
            val unscaledHeightDp = overlaySize.height.toFloat() / density / previewScale
            val frameBounds = ClockEditFrameBounds.from(editModel.scaleGeometry, unscaledWidthDp)
            val frameStartDp =
                (frameBounds.startOffsetDp(
                    unscaledWidthDp,
                    editModel.horizontalOffsetDp,
                ) * previewScale).dp
            val frameTopDp = (frameBounds.topOffsetDp(unscaledHeightDp) * previewScale).dp
            val frameEndDp =
                frameBounds.endOffsetDp(
                    unscaledWidthDp,
                    editModel.horizontalOffsetDp,
                ) * previewScale
            val frameBottomDp = frameBounds.bottomOffsetDp(unscaledHeightDp) * previewScale

            PreviewClock(
                isPreview = isPreview,
                isRegionDark = isRegionDark,
                depthSourceBoundsProvider = depthSourceBoundsProvider,
                depthSourceScale = depthSourceScale,
                verticalPadding = if (handlesVisible) 4.dp else null,
                fitClockBounds = handlesVisible,
                sizeScaleOverride = previewSizeScaleOverride,
                horizontalOffsetDpOverride = previewHorizontalOffsetOverride,
                freezePreviewAlignment = freezePreviewAlignment,
                depthEffectVisible = depthEffectVisible,
                animationTrigger = clockAnimationTrigger,
                freezeEditGeometry = freezeEditGeometry,
                onEditGeometryChanged = { scaleGeometry = it },
            )

            if (handlesVisible) {
                ClockResizeCorner.entries.forEach { corner ->
                    ResizeCornerHandle(
                        modifier =
                            Modifier.align(Alignment.TopStart)
                                .offset(
                                    x = corner.handleX(
                                        frameStartDp.value,
                                        frameEndDp,
                                        CLOCK_EDIT_HANDLE_SIZE_DP.toFloat(),
                                        CLOCK_EDIT_HANDLE_OVERFLOW_DP.toFloat(),
                                    ).dp,
                                    y = corner.handleY(
                                        frameTopDp.value,
                                        frameBottomDp,
                                        CLOCK_EDIT_HANDLE_SIZE_DP.toFloat(),
                                        CLOCK_EDIT_HANDLE_OVERFLOW_DP.toFloat(),
                                    ).dp,
                                )
                                .zIndex(2f)
                                .size(CLOCK_EDIT_HANDLE_SIZE_DP.dp),
                        corner = corner,
                        color = handleColor,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CornerResizeHandle(
    modifier: Modifier,
    corner: ClockResizeCorner,
    onDragStart: () -> Unit,
    onDragCancel: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    color: Color,
    visualScale: Float = 1f,
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDrag by rememberUpdatedState(onDrag)
    ResizeCornerHandle(
        modifier =
            modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentOnDragStart() },
                    onDragCancel = { currentOnDragCancel() },
                    onDragEnd = { currentOnDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.x, dragAmount.y)
                    },
                )
            },
        corner = corner,
        color = color,
        visualScale = visualScale,
    )
}

@Composable
private fun ResizeCornerHandle(
    modifier: Modifier,
    corner: ClockResizeCorner,
    color: Color,
    visualScale: Float = 1f,
) {
    Canvas(
        modifier = modifier.systemGestureExclusion {
            Rect(0f, 0f, it.size.width.toFloat(), it.size.height.toFloat())
        }
    ) {
        val strokeWidth = (CLOCK_EDIT_HANDLE_STROKE_DP * visualScale).dp.toPx()
        val inset = (CLOCK_EDIT_HANDLE_INSET_DP * visualScale).dp.toPx()
        val arcSize = (CLOCK_EDIT_HANDLE_ARC_SIZE_DP * visualScale).dp.toPx()
        val arcLeft = if (corner.isLeft) inset else size.width - arcSize - inset
        val arcTop = if (corner.isTop) inset else size.height - arcSize - inset
        drawArc(
            color = color,
            startAngle = corner.arcStartAngle,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(arcLeft, arcTop),
            size = Size(arcSize, arcSize),
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
    }
}
