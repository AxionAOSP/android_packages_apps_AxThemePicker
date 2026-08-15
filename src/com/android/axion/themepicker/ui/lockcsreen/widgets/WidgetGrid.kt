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

package com.android.axion.themepicker.ui.lockscreen.widgets

import android.appwidget.AppWidgetProviderInfo
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.lockscreen.CLOCK_EDIT_HANDLE_SIZE_DP
import com.android.axion.themepicker.ui.lockscreen.ClockResizeCorner
import com.android.axion.themepicker.ui.lockscreen.CornerResizeHandle
import com.android.axion.themepicker.ui.lockscreen.Dimens
import com.android.axion.themepicker.ui.lockscreen.previewScale
import com.android.axion.themepicker.utils.math.scaleRatio
import kotlin.math.roundToInt

private fun occupancyExcluding(
    widgets: List<GridWidgetItem>,
    excludeId: Int,
): Array<BooleanArray> {
    val matrix = Array(MAX_ROWS) { BooleanArray(GRID_COLUMNS) }
    widgets.forEach { w ->
        if (w.appWidgetId == excludeId) return@forEach
        val yEnd = (w.cellY + w.spanY).coerceAtMost(MAX_ROWS)
        val xEnd = (w.cellX + w.spanX).coerceAtMost(GRID_COLUMNS)
        for (y in w.cellY until yEnd) {
            for (x in w.cellX until xEnd) {
                if (y in 0 until MAX_ROWS && x in 0 until GRID_COLUMNS) matrix[y][x] = true
            }
        }
    }
    return matrix
}

private fun maxSpanXFor(
    occupancy: Array<BooleanArray>,
    cellX: Int,
    cellY: Int,
    spanY: Int,
    desiredX: Int,
): Int {
    val maxX = desiredX.coerceIn(1, GRID_COLUMNS - cellX)
    for (sx in 1..maxX) {
        val x = cellX + sx - 1
        for (y in cellY until (cellY + spanY).coerceAtMost(MAX_ROWS)) {
            if (y !in 0 until MAX_ROWS || x !in 0 until GRID_COLUMNS || occupancy[y][x]) {
                return (sx - 1).coerceAtLeast(1)
            }
        }
    }
    return maxX
}

private fun maxSpanYFor(
    occupancy: Array<BooleanArray>,
    cellX: Int,
    cellY: Int,
    spanX: Int,
    desiredY: Int,
): Int {
    val maxY = desiredY.coerceIn(1, MAX_ROWS - cellY)
    for (sy in 1..maxY) {
        val y = cellY + sy - 1
        for (x in cellX until (cellX + spanX).coerceAtMost(GRID_COLUMNS)) {
            if (y !in 0 until MAX_ROWS || x !in 0 until GRID_COLUMNS || occupancy[y][x]) {
                return (sy - 1).coerceAtLeast(1)
            }
        }
    }
    return maxY
}

private fun clampNoCollision(
    occupancy: Array<BooleanArray>,
    cellX: Int,
    cellY: Int,
    currentSpanY: Int,
    desiredX: Int,
    desiredY: Int,
): Pair<Int, Int> {
    val spanYForHorizontalCheck = minOf(currentSpanY, desiredY)
    val nx = maxSpanXFor(occupancy, cellX, cellY, spanYForHorizontalCheck, desiredX)
    val ny = maxSpanYFor(occupancy, cellX, cellY, nx, desiredY)
    return nx to ny
}

private class WidgetResizeSession {
    private lateinit var widget: GridWidgetItem
    private lateinit var occupancy: Array<BooleanArray>
    private var totalDelta = Offset.Zero

    fun start(widget: GridWidgetItem, widgets: List<GridWidgetItem>) {
        this.widget = widget
        occupancy = occupancyExcluding(widgets, widget.appWidgetId)
        totalDelta = Offset.Zero
    }

    fun resizeBy(
        delta: Offset,
        cellStepPx: Float,
        horizontal: Boolean,
        vertical: Boolean,
    ): Pair<Int, Int> {
        totalDelta += delta
        val desiredX =
            if (horizontal) {
                (widget.spanX + (totalDelta.x / cellStepPx).roundToInt())
                    .coerceIn(1, GRID_COLUMNS - widget.cellX)
            } else {
                widget.spanX
            }
        val desiredY =
            if (vertical) {
                (widget.spanY + (totalDelta.y / cellStepPx).roundToInt())
                    .coerceIn(1, MAX_ROWS - widget.cellY)
            } else {
                widget.spanY
            }
        return clampNoCollision(
            occupancy = occupancy,
            cellX = widget.cellX,
            cellY = widget.cellY,
            currentSpanY = widget.spanY,
            desiredX = desiredX,
            desiredY = desiredY,
        )
    }
}

private val WidgetPlacementSpec =
    spring<Float>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioLowBouncy,
    )

@Composable
fun WidgetGrid(
    isPreview: Boolean = false,
    widgets: List<GridWidgetItem>,
    onRemove: (GridWidgetItem) -> Unit,
    onPickWidget: () -> Unit,
    onConfigure: ((GridWidgetItem) -> Unit)? = null,
    onResizeWidget: ((GridWidgetItem) -> Unit)? = null,
    dragDropState: WidgetDragDropState? = null,
    onWidgetsMoved: ((List<GridWidgetItem>) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scale = if (isPreview) context.previewScale else context.scaleRatio

    val cellSize = Dimens.WidgetCellSize * scale
    val gap = Dimens.WidgetCellGap * scale
    val cornerRadius = Dimens.WidgetCellCorner * scale
    val gridWidth = cellSize * GRID_COLUMNS + gap * (GRID_COLUMNS - 1)
    val gridHeight = cellSize * MAX_ROWS + gap * (MAX_ROWS - 1)

    val displayWidgets =
        if (dragDropState?.dragInProgress == true) {
            dragDropState.previewWidgets
        } else {
            widgets
        }

    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSize.toPx() }
    val gapPx = with(density) { gap.toPx() }
    var resizingId by remember { mutableIntStateOf(-1) }
    var resizingSpanX by remember { mutableIntStateOf(1) }
    var resizingSpanY by remember { mutableIntStateOf(1) }
    var selectedId by remember { mutableIntStateOf(-1) }

    Box(
        modifier =
            Modifier.requiredSize(width = gridWidth, height = gridHeight)
                .then(
                    if (!isPreview) {
                        Modifier.drawWithContent {
                            drawContent()
                            val strokeWidth = 1.dp.toPx()
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.42f),
                                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                                size =
                                    Size(
                                        width = size.width - strokeWidth,
                                        height = size.height - strokeWidth,
                                    ),
                                cornerRadius = CornerRadius(cornerRadius.toPx()),
                                style = Stroke(strokeWidth),
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (!isPreview)
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onPickWidget,
                        )
                    else Modifier
                ),
        contentAlignment = Alignment.TopStart,
    ) {
        if (!isPreview && widgets.isEmpty()) {
            Text(
                text = stringResource(R.string.add_widgets),
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (dragDropState != null && dragDropState.dragInProgress) {
            val target = dragDropState.dropTarget
            if (target != null) {
                val dragId = dragDropState.draggingWidgetId
                val dragged = widgets.firstOrNull { it.appWidgetId == dragId }
                if (dragged != null) {
                    val tx = (cellSize + gap) * target.cellX
                    val ty = (cellSize + gap) * target.cellY
                    val tw = cellSize * dragged.spanX + gap * (dragged.spanX - 1).coerceAtLeast(0)
                    val th = cellSize * dragged.spanY + gap * (dragged.spanY - 1).coerceAtLeast(0)
                    val highlightColor = if (target.isValid) Color.White else Color.Red

                    Box(
                        modifier =
                            Modifier.offset(x = tx, y = ty)
                                .size(width = tw, height = th)
                                .zIndex(3f)
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(highlightColor.copy(alpha = 0.20f))
                                .border(
                                    width = 1.5.dp * scale,
                                    color = highlightColor.copy(
                                        alpha = if (target.isValid) 0.72f else 0.4f,
                                    ),
                                    shape = RoundedCornerShape(cornerRadius),
                                )
                    )
                }
            }
        }

        displayWidgets.forEach { widget ->
            val isResizing = resizingId == widget.appWidgetId
            val effSpanX = if (isResizing) resizingSpanX else widget.spanX
            val effSpanY = if (isResizing) resizingSpanY else widget.spanY
            val targetX = widget.cellX * (cellSizePx + gapPx)
            val targetY = widget.cellY * (cellSizePx + gapPx)
            val w = cellSize * effSpanX + gap * (effSpanX - 1).coerceAtLeast(0)
            val h = cellSize * effSpanY + gap * (effSpanY - 1).coerceAtLeast(0)
            val isBeingDragged = dragDropState?.isDragging(widget) == true

            key(widget.appWidgetId, widget.provider) {
                val animatedX by
                    animateFloatAsState(
                        targetValue = targetX,
                        animationSpec = WidgetPlacementSpec,
                        label = "widgetX",
                    )
                val animatedY by
                    animateFloatAsState(
                        targetValue = targetY,
                        animationSpec = WidgetPlacementSpec,
                        label = "widgetY",
                    )

                val providerInfo =
                    remember(widget.provider) { widget.providerInfo(context) }

                val previewBitmap =
                    remember(widget.provider) {
                        providerInfo
                            ?.loadPreviewImage(
                                context,
                                context.resources.displayMetrics.densityDpi,
                            )
                            ?.let { drawable ->
                                val iw =
                                    drawable.intrinsicWidth.takeIf { it > 0 }
                                        ?: (200 * context.resources.displayMetrics.density).toInt()
                                val ih =
                                    drawable.intrinsicHeight.takeIf { it > 0 }
                                        ?: (100 * context.resources.displayMetrics.density).toInt()
                                drawable.toBitmap(iw, ih)
                            }
                    }
                val hasPreviewLayout = providerInfo != null && providerInfo.previewLayout != 0
                val iconDrawable = remember(widget.provider) { widget.icon(context) }
                val labelText = remember(widget.provider) { widget.label(context) }

                val isSelected = selectedId == widget.appWidgetId
                val resizeHandleColor = MaterialTheme.colorScheme.primary
                val resizeFrameColor = resizeHandleColor.copy(alpha = 0.55f)
                val interactionSrc = remember(widget.appWidgetId) { MutableInteractionSource() }
                val currentWidgetsState by rememberUpdatedState(widgets)
                val currentWidgetForDrag by rememberUpdatedState(widget)
                val selectableModifier =
                    if (isPreview) Modifier
                    else
                        Modifier.selectable(
                            selected = isSelected,
                            interactionSource = interactionSrc,
                            indication = null,
                            onClick = { selectedId = if (isSelected) -1 else widget.appWidgetId },
                        )
                val dragModifier =
                    if (isPreview || dragDropState == null) Modifier
                    else {
                        val dds = dragDropState
                        Modifier.pointerInput(widget.appWidgetId, dds, cellSizePx, gapPx) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    val wItem = currentWidgetForDrag
                                    selectedId = wItem.appWidgetId
                                    dds.onDragStart(wItem, currentWidgetsState)
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    dds.onDrag(delta, cellSizePx, gapPx)
                                },
                                onDragEnd = {
                                    dds.onDragEnd()?.also { onWidgetsMoved?.invoke(it) }
                                },
                                onDragCancel = { dds.onCancelled() },
                            )
                        }
                    }
                val draggingOffsetModifier =
                    if (isBeingDragged && dragDropState != null) {
                        val dds = dragDropState
                        Modifier.graphicsLayer {
                            translationX = dds.draggingItemOffset.x
                            translationY = dds.draggingItemOffset.y
                        }.zIndex(4f)
                    } else Modifier
                val selectionModifier =
                    if (isSelected)
                        Modifier.drawWithContent {
                            drawContent()
                            val strokeWidth = 2.dp.toPx() * scale
                            drawRoundRect(
                                color = resizeFrameColor,
                                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                                size =
                                    Size(
                                        width = size.width - strokeWidth,
                                        height = size.height - strokeWidth,
                                    ),
                                cornerRadius = CornerRadius(cornerRadius.toPx()),
                                style = Stroke(strokeWidth),
                            )
                        }
                    else Modifier
                val offsetX = if (isBeingDragged) targetX else animatedX
                val offsetY = if (isBeingDragged) targetY else animatedY
                val cellBgModifier =
                    Modifier.clip(RoundedCornerShape(cornerRadius))
                        .background(Color.White.copy(alpha = 0.22f))
                Box(
                    modifier =
                        Modifier.offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                            .size(width = w, height = h)
                            .then(draggingOffsetModifier)
                            .then(cellBgModifier)
                            .then(selectionModifier)
                            .then(selectableModifier)
                            .then(dragModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = labelText,
                            modifier = Modifier.fillMaxSize().padding(2.dp * scale),
                            contentScale = ContentScale.Fit,
                        )
                    } else if (hasPreviewLayout && providerInfo != null) {
                        var layoutFailed by remember { mutableStateOf(false) }
                        if (!layoutFailed) {
                            AndroidView(
                                factory = { ctx ->
                                    WidgetPreviewHostView(ctx).apply {
                                        try {
                                            setAppWidget(-1, providerInfo)
                                            val rv =
                                                RemoteViews(
                                                    providerInfo.provider.packageName,
                                                    providerInfo.previewLayout,
                                                )
                                            updateAppWidget(rv)
                                        } catch (_: Exception) {
                                            layoutFailed = true
                                        }
                                        post { if (childCount == 0) layoutFailed = true }
                                    }
                                },
                                modifier = Modifier.fillMaxSize().padding(2.dp * scale),
                                update = { view ->
                                    view.setContainerSizePx(
                                        (w.value * context.resources.displayMetrics.density)
                                            .toInt(),
                                        (h.value * context.resources.displayMetrics.density)
                                            .toInt(),
                                    )
                                    view.requestLayout()
                                },
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp * scale),
                        ) {
                            iconDrawable?.let { drawable ->
                                Image(
                                    bitmap =
                                        drawable
                                            .toBitmap(
                                                width =
                                                    (24 * context.resources.displayMetrics.density)
                                                        .toInt(),
                                                height =
                                                    (24 * context.resources.displayMetrics.density)
                                                        .toInt(),
                                            )
                                            .asImageBitmap(),
                                    contentDescription = labelText,
                                    modifier = Modifier.size(20.dp * scale),
                                )
                            }
                                ?: Icon(
                                    Icons.Default.Widgets,
                                    contentDescription = labelText,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp * scale),
                                )

                            if (widget.spanX > 1 || widget.spanY > 1) {
                                Spacer(Modifier.height(2.dp * scale))
                                Text(
                                    text = labelText,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 9.sp * scale,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (!isPreview && isSelected) {
                        val badgeSize = 24.dp * scale
                        val iconSize = 16.dp * scale
                        val badgeOffset = 4.dp * scale
                        val badgeBg = MaterialTheme.colorScheme.primary
                        val badgeTint = MaterialTheme.colorScheme.onPrimary
                        val showConfigure =
                            onConfigure != null &&
                                providerInfo != null &&
                                providerInfo.configure != null
                        Row(
                            modifier =
                                Modifier.align(Alignment.TopEnd)
                                    .zIndex(4f)
                                    .padding(badgeOffset),
                            horizontalArrangement = Arrangement.spacedBy(4.dp * scale),
                        ) {
                            if (showConfigure) {
                                Box(
                                    modifier =
                                        Modifier.size(badgeSize)
                                            .clip(CircleShape)
                                            .background(badgeBg)
                                            .clickable { onConfigure!!(widget) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = badgeTint,
                                        modifier = Modifier.size(iconSize),
                                    )
                                }
                            }
                            Box(
                                modifier =
                                    Modifier.size(badgeSize)
                                        .clip(CircleShape)
                                        .background(badgeBg)
                                        .clickable { onRemove(widget) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = badgeTint,
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                        }

                        if (onResizeWidget != null) {
                            val cellFullPx = cellSizePx + gapPx
                            val siblings by rememberUpdatedState(widgets)
                            val currentWidget by rememberUpdatedState(widget)
                            val resizeSession =
                                remember(widget.appWidgetId) { WidgetResizeSession() }
                            val resizeMode =
                                providerInfo?.resizeMode ?: AppWidgetProviderInfo.RESIZE_BOTH
                            val horizontal =
                                (resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0
                            val vertical =
                                (resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0
                            val occupancy =
                                remember(widgets, widget.appWidgetId) {
                                    occupancyExcluding(widgets, widget.appWidgetId)
                                }
                            val maxSpanX =
                                remember(occupancy, widget.cellX, widget.cellY, widget.spanY) {
                                    maxSpanXFor(
                                        occupancy,
                                        widget.cellX,
                                        widget.cellY,
                                        widget.spanY,
                                        GRID_COLUMNS - widget.cellX,
                                    )
                                }
                            val maxSpanY =
                                remember(occupancy, widget.cellX, widget.cellY, widget.spanX) {
                                    maxSpanYFor(
                                        occupancy,
                                        widget.cellX,
                                        widget.cellY,
                                        widget.spanX,
                                        MAX_ROWS - widget.cellY,
                                    )
                                }
                            val canResizeHorizontally =
                                horizontal && (maxSpanX > widget.spanX || widget.spanX > 1)
                            val canResizeVertically =
                                vertical && (maxSpanY > widget.spanY || widget.spanY > 1)

                            if (canResizeHorizontally || canResizeVertically) {
                                CornerResizeHandle(
                                    modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                            .zIndex(5f)
                                            .size((CLOCK_EDIT_HANDLE_SIZE_DP * scale).dp),
                                    corner = ClockResizeCorner.BottomRight,
                                    onDragStart = {
                                        val current = currentWidget
                                        resizeSession.start(current, siblings)
                                        resizingId = current.appWidgetId
                                        resizingSpanX = current.spanX
                                        resizingSpanY = current.spanY
                                    },
                                    onDragCancel = { resizingId = -1 },
                                    onDragEnd = {
                                        val spanX = resizingSpanX
                                        val spanY = resizingSpanY
                                        resizingId = -1
                                        val current = currentWidget
                                        if (spanX != current.spanX || spanY != current.spanY) {
                                            onResizeWidget(
                                                current.copy(
                                                    spanX = spanX,
                                                    spanY = spanY,
                                                )
                                            )
                                        }
                                    },
                                    onDrag = { dx, dy ->
                                        val (spanX, spanY) =
                                            resizeSession.resizeBy(
                                                delta = Offset(dx, dy),
                                                cellStepPx = cellFullPx,
                                                horizontal = canResizeHorizontally,
                                                vertical = canResizeVertically,
                                            )
                                        resizingSpanX = spanX
                                        resizingSpanY = spanY
                                    },
                                    color = resizeHandleColor,
                                    visualScale = scale,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
