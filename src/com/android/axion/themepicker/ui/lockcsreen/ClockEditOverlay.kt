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
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.lockscreen.widgets.MAX_ROWS
import com.android.axion.themepicker.ui.lockscreen.widgets.observeTaps
import com.android.axion.themepicker.utils.math.scaleRatio
import com.android.systemui.shared.clocks.ClockEditScaleGeometry
import com.android.systemui.shared.clocks.ClockSettingsRepository
import com.android.systemui.shared.clocks.ClockTopPaddingRange
import com.android.systemui.shared.clocks.ClockWidgetGridMetrics
import com.android.systemui.shared.clocks.ClockWidgetLayoutState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CLOCK_EDIT_EDUCATION_PREFS = "clock_edit_education"
private const val CLOCK_EDIT_EDUCATION_SHOWN = "clock_edit_education_shown"

@Composable
fun EditablePreviewClock(
    isPreview: Boolean,
    isRegionDark: Boolean = true,
    editable: Boolean,
    modifier: Modifier = Modifier,
    depthSourceBoundsProvider: (() -> RectF?)? = null,
    onClick: (() -> Unit)? = null,
    applyFluidSize: Boolean = true,
    applyFluidTopPadding: Boolean = true,
    depthEffectEnabled: Boolean = true,
    clockAnimationTrigger: Int = 0,
    lockscreenWidgetLayoutState: ClockWidgetLayoutState = ClockWidgetLayoutState.Empty,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val previewScale = if (isPreview) context.previewScale else context.scaleRatio

    LaunchedEffect(context) {
        ClockSettingsRepository.init(context)
    }

    val repositorySizeScale by ClockSettingsRepository.sizeScale.collectAsState()
    val repositoryTopPadding by ClockSettingsRepository.topPaddingDp.collectAsState()
    val repositoryAlignment by ClockSettingsRepository.resolvedClockAlignment.collectAsState()
    val storedSizeScale = if (applyFluidSize) repositorySizeScale else 1f
    val storedTopPadding = if (applyFluidTopPadding) repositoryTopPadding else 0f
    val availableClockWidthDp = configuration.screenWidthDp / previewScale
    val fallbackScaleGeometry = remember(availableClockWidthDp, storedSizeScale) {
        ClockEditScaleGeometry.default(
            availableWidthDp = availableClockWidthDp,
            requestedScale = storedSizeScale,
            scaleRange = ClockSettingsRepository.sizeScaleRange,
        )
    }
    var scaleGeometry by remember { mutableStateOf(fallbackScaleGeometry) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(fallbackScaleGeometry) {
        scaleGeometry = fallbackScaleGeometry
    }

    val topPaddingRange = remember(lockscreenWidgetLayoutState) {
        val reservedHeight =
            ClockWidgetGridMetrics(
                cellSizeDp = Dimens.WidgetCellSize.value,
                cellGapDp = Dimens.WidgetCellGap.value,
                minimumRows = if (lockscreenWidgetLayoutState.hasWidgets) MAX_ROWS else 0,
            ).reservedHeightDp(lockscreenWidgetLayoutState)
        ClockTopPaddingRange(
            min = CLOCK_EDIT_TOP_PADDING_MIN_DP,
            baseMax = ClockSettingsRepository.TOP_PADDING_MAX_DP,
            reservedHeightDp = reservedHeight,
        )
    }
    var editModel by remember {
        mutableStateOf(
            ClockEditModel.from(
                storedSizeScale = storedSizeScale,
                storedTopPaddingDp = storedTopPadding,
                alignmentValue = repositoryAlignment,
                scaleGeometry = scaleGeometry,
                topPaddingRange = topPaddingRange,
            )
        )
    }

    LaunchedEffect(
        storedSizeScale,
        storedTopPadding,
        repositoryAlignment,
        scaleGeometry,
        topPaddingRange,
    ) {
        editModel = editModel.sync(
            storedSizeScale = storedSizeScale,
            storedTopPaddingDp = storedTopPadding,
            alignmentValue = repositoryAlignment,
            scaleGeometry = scaleGeometry,
            topPaddingRange = topPaddingRange,
        )
    }

    val topPadding = when {
        editable -> editModel.topPaddingDp
        applyFluidTopPadding -> topPaddingRange.clamp(storedTopPadding)
        else -> storedTopPadding
    }
    val topPaddingDp = (topPadding.coerceAtLeast(0f) * previewScale).dp
    val topOffsetDp = (topPadding.coerceAtMost(0f) * previewScale).dp
    val handlesVisible = editable && !isPreview
    var educationVisible by remember(context) {
        mutableStateOf(!isClockEditEducationShown(context))
    }
    val previewSizeScaleOverride = when {
        !applyFluidSize -> 1f
        handlesVisible -> editModel.sizeScaleOverride
        else -> null
    }
    val depthEffectVisible =
        depthEffectEnabled &&
            (!handlesVisible || editModel.dragTarget == ClockEditDragTarget.None)
    val highlightColor = Color.White

    fun endSizeDrag() {
        val value = editModel.sizeScaleOverride
        scope.launch(Dispatchers.IO) { writeSizeScale(context, value) }
        editModel = editModel.commitDrag()
    }

    fun cancelSizeDrag() {
        editModel = editModel.cancelDrag()
    }

    fun cancelTopPaddingDrag() {
        editModel = editModel.cancelDrag()
    }

    fun endTopPaddingDrag() {
        val value = editModel.topPaddingDp
        scope.launch(Dispatchers.IO) { writeTopPadding(context, value) }
        editModel = editModel.commitDrag()
    }

    val latestEditModel by rememberUpdatedState(editModel)
    val latestOnClick by rememberUpdatedState(onClick)

    LaunchedEffect(context, handlesVisible, educationVisible) {
        if (handlesVisible && educationVisible) {
            setClockEditEducationShown(context)
        }
    }

    fun isResizeHandleHit(offset: Offset, widthPx: Float, heightPx: Float): Boolean {
        val model = latestEditModel
        val handleSizePx = CLOCK_EDIT_HANDLE_SIZE_DP * density
        val unscaledWidthDp = widthPx / density / previewScale
        val unscaledHeightDp = heightPx / density / previewScale
        val frameBounds = ClockEditFrameBounds.from(model.scaleGeometry, unscaledWidthDp)
        val handleStartPx =
            frameBounds.handleStartOffsetDp(
                unscaledWidthDp,
                model.alignment,
            ) * previewScale * density
        val handleEndPx = handleStartPx + handleSizePx
        val handleTopPx =
            frameBounds.handleTopOffsetDp(unscaledHeightDp) * previewScale * density
        val handleBottomPx = handleTopPx + handleSizePx
        return offset.x in handleStartPx..handleEndPx && offset.y in handleTopPx..handleBottomPx
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
                                        if (!isResizeHandleHit(
                                                offset,
                                                size.width.toFloat(),
                                                size.height.toFloat(),
                                            )
                                        ) {
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
                                var positionDragMoved = false
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        positionDragActive =
                                            !isResizeHandleHit(
                                                offset,
                                                size.width.toFloat(),
                                                size.height.toFloat(),
                                            )
                                        positionDragMoved = false
                                        if (positionDragActive) {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.LongPress,
                                            )
                                            editModel = editModel.beginPositionDrag()
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (positionDragActive) {
                                            change.consume()
                                            if (dragAmount.y != 0f) {
                                                positionDragMoved = true
                                                editModel =
                                                    editModel.moveTopPaddingBy(
                                                        dragAmount.y,
                                                        density,
                                                        previewScale,
                                                    )
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (positionDragActive) {
                                            if (positionDragMoved) {
                                                endTopPaddingDrag()
                                            } else {
                                                editModel = editModel.finishDrag()
                                            }
                                        }
                                        positionDragActive = false
                                        positionDragMoved = false
                                    },
                                    onDragCancel = {
                                        if (positionDragActive) cancelTopPaddingDrag()
                                        positionDragActive = false
                                        positionDragMoved = false
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
                                        editModel.alignment,
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
                                    color = highlightColor,
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
            val frameWidthDp = (frameBounds.widthIn(unscaledWidthDp) * previewScale).dp
            val frameStartDp =
                frameBounds.startOffsetDp(unscaledWidthDp, editModel.alignment).let {
                    (it * previewScale).dp
                }
            val frameTopDp = (frameBounds.topOffsetDp(unscaledHeightDp) * previewScale).dp
            val handleStartDp =
                frameBounds.handleStartOffsetDp(unscaledWidthDp, editModel.alignment).let {
                    (it * previewScale).dp
                }
            val handleTopDp =
                (frameBounds.handleTopOffsetDp(unscaledHeightDp) * previewScale).dp

            PreviewClock(
                isPreview = isPreview,
                isRegionDark = isRegionDark,
                depthSourceBoundsProvider = depthSourceBoundsProvider,
                verticalPadding = if (handlesVisible) 4.dp else null,
                fitClockBounds = handlesVisible,
                sizeScaleOverride = previewSizeScaleOverride,
                depthEffectVisible = depthEffectVisible,
                animationTrigger = clockAnimationTrigger,
                onEditGeometryChanged = { scaleGeometry = it },
            )

            if (handlesVisible) {
                ClockResetButton(
                    modifier =
                        Modifier.align(Alignment.TopStart)
                            .offset(x = frameStartDp, y = frameTopDp)
                            .width(frameWidthDp)
                            .padding(top = 8.dp, end = 8.dp)
                            .zIndex(3f),
                    onClick = {
                        editModel = editModel.resetSize().resetPosition()
                        scope.launch(Dispatchers.IO) {
                            writeSizeScale(context, 1f)
                            writeTopPadding(context, 0f)
                        }
                    },
                )
                CornerResizeHandle(
                    modifier =
                        Modifier.align(Alignment.TopStart)
                            .offset(x = handleStartDp, y = handleTopDp)
                            .zIndex(2f)
                            .size(CLOCK_EDIT_HANDLE_SIZE_DP.dp),
                    onDragStart = { editModel = editModel.beginSizeDrag() },
                    onDragCancel = { cancelSizeDrag() },
                    onDragEnd = { endSizeDrag() },
                    onDrag = { dx, dy ->
                        editModel = editModel.resizeBy(dx, dy, density, previewScale)
                    },
                    color = highlightColor,
                )
            }
        }
        if (handlesVisible && educationVisible) {
            ClockEditEducationPill(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .offset(y = 8.dp)
                        .padding(horizontal = 24.dp)
                        .zIndex(4f),
                onDismiss = {
                    educationVisible = false
                    setClockEditEducationShown(context)
                },
            )
        }
    }
}

@Composable
private fun ClockEditEducationPill(
    modifier: Modifier,
    onDismiss: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.clock_edit_education),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Start,
                maxLines = 3,
            )
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = onDismiss,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ClockResetButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopEnd) {
        FilledIconButton(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            onClick = onClick,
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.reset_clock),
            )
        }
    }
}

@Composable
private fun CornerResizeHandle(
    modifier: Modifier,
    onDragStart: () -> Unit,
    onDragCancel: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    color: Color,
) {
    Canvas(
        modifier =
            modifier
                .systemGestureExclusion {
                    Rect(0f, 0f, it.size.width.toFloat(), it.size.height.toFloat())
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragCancel = onDragCancel,
                        onDragEnd = onDragEnd,
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        },
                    )
                }
    ) {
        val strokeWidth = CLOCK_EDIT_HANDLE_STROKE_DP.dp.toPx()
        val inset = CLOCK_EDIT_HANDLE_INSET_DP.dp.toPx()
        val arcSize = CLOCK_EDIT_HANDLE_ARC_SIZE_DP.dp.toPx()
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width - arcSize - inset, size.height - arcSize - inset),
            size = Size(arcSize, arcSize),
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
    }
}

private fun writeSizeScale(context: Context, value: Float) {
    Settings.Secure.putString(
        context.contentResolver,
        ClockSettingsRepository.SETTING_SIZE_SCALE,
        value.toString(),
    )
}

private fun writeTopPadding(context: Context, value: Float) {
    Settings.Secure.putString(
        context.contentResolver,
        ClockSettingsRepository.SETTING_TOP_PADDING,
        value.toString(),
    )
}

private fun isClockEditEducationShown(context: Context): Boolean =
    context.getSharedPreferences(CLOCK_EDIT_EDUCATION_PREFS, Context.MODE_PRIVATE)
        .getBoolean(CLOCK_EDIT_EDUCATION_SHOWN, false)

private fun setClockEditEducationShown(context: Context) {
    context.getSharedPreferences(CLOCK_EDIT_EDUCATION_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(CLOCK_EDIT_EDUCATION_SHOWN, true)
        .apply()
}
