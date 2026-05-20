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

import com.android.systemui.shared.clocks.ClockEditScaleGeometry
import com.android.systemui.shared.clocks.ClockSettingsRepository
import com.android.systemui.shared.clocks.ClockTopPaddingRange
import kotlin.math.abs

internal const val CLOCK_EDIT_HANDLE_SIZE_DP = 96
internal const val CLOCK_EDIT_GUIDE_CORNER_RADIUS_DP = 24
internal const val CLOCK_EDIT_GUIDE_STROKE_DP = 4
internal const val CLOCK_EDIT_HANDLE_STROKE_DP = 8
internal const val CLOCK_EDIT_HANDLE_ARC_SIZE_DP = CLOCK_EDIT_GUIDE_CORNER_RADIUS_DP * 2
internal const val CLOCK_EDIT_HANDLE_INSET_DP = CLOCK_EDIT_HANDLE_STROKE_DP / 2
internal const val CLOCK_EDIT_TOP_PADDING_MIN_DP = 0f

internal data class ClockEditModel(
    val storedSizeScale: Float,
    val editedSizeScale: Float,
    val storedTopPaddingDp: Float,
    val editedTopPaddingDp: Float,
    val alignment: ClockEditAlignment,
    val scaleGeometry: ClockEditScaleGeometry,
    val topPaddingRange: ClockTopPaddingRange,
    val dragTarget: ClockEditDragTarget = ClockEditDragTarget.None,
    val pendingSizeScale: Float? = null,
    val pendingTopPaddingDp: Float? = null,
) {
    val sizeScaleOverride: Float
        get() = scaleGeometry.clampScale(editedSizeScale)

    val topPaddingDp: Float
        get() = topPaddingRange.clamp(editedTopPaddingDp)

    fun sync(
        storedSizeScale: Float,
        storedTopPaddingDp: Float,
        alignmentValue: String,
        scaleGeometry: ClockEditScaleGeometry,
        topPaddingRange: ClockTopPaddingRange,
    ): ClockEditModel {
        val nextAlignment = ClockEditAlignment.fromSetting(alignmentValue)
        val nextPendingSizeScale =
            pendingSizeScale?.takeUnless { storedSizeScale.approximatelyEquals(it) }
        val nextPendingTopPadding =
            pendingTopPaddingDp?.takeUnless { storedTopPaddingDp.approximatelyEquals(it) }
        val nextStoredSizeScale = nextPendingSizeScale ?: storedSizeScale
        val nextStoredTopPadding = nextPendingTopPadding ?: storedTopPaddingDp
        val nextScaleGeometry =
            if (
                dragTarget == ClockEditDragTarget.Size &&
                    !scaleGeometry.requestedScale.approximatelyEquals(
                        scaleGeometry.clampScale(editedSizeScale),
                    )
            ) {
                this.scaleGeometry
            } else {
                scaleGeometry
            }
        if (dragTarget != ClockEditDragTarget.None) {
            return copy(
                storedSizeScale = nextStoredSizeScale,
                editedSizeScale = nextScaleGeometry.clampScale(editedSizeScale),
                storedTopPaddingDp = nextStoredTopPadding,
                editedTopPaddingDp = topPaddingRange.clamp(editedTopPaddingDp),
                alignment = nextAlignment,
                scaleGeometry = nextScaleGeometry,
                topPaddingRange = topPaddingRange,
                pendingSizeScale = nextPendingSizeScale,
                pendingTopPaddingDp = nextPendingTopPadding,
            )
        }
        return copy(
            storedSizeScale = nextStoredSizeScale,
            editedSizeScale = scaleGeometry.clampScale(nextStoredSizeScale),
            storedTopPaddingDp = nextStoredTopPadding,
            editedTopPaddingDp = topPaddingRange.clamp(nextStoredTopPadding),
            alignment = nextAlignment,
            scaleGeometry = scaleGeometry,
            topPaddingRange = topPaddingRange,
            pendingSizeScale = nextPendingSizeScale,
            pendingTopPaddingDp = nextPendingTopPadding,
        )
    }

    fun beginSizeDrag(): ClockEditModel = copy(dragTarget = ClockEditDragTarget.Size)

    fun beginPositionDrag(): ClockEditModel = copy(dragTarget = ClockEditDragTarget.Position)

    fun cancelDrag(): ClockEditModel {
        return when (dragTarget) {
            ClockEditDragTarget.Size -> copy(
                editedSizeScale = scaleGeometry.clampScale(storedSizeScale),
                dragTarget = ClockEditDragTarget.None,
            )
            ClockEditDragTarget.Position -> copy(
                editedTopPaddingDp = topPaddingRange.clamp(storedTopPaddingDp),
                dragTarget = ClockEditDragTarget.None,
            )
            ClockEditDragTarget.None -> this
        }
    }

    fun finishDrag(): ClockEditModel = copy(dragTarget = ClockEditDragTarget.None)

    fun commitDrag(): ClockEditModel {
        return when (dragTarget) {
            ClockEditDragTarget.Size -> copy(
                storedSizeScale = sizeScaleOverride,
                editedSizeScale = sizeScaleOverride,
                dragTarget = ClockEditDragTarget.None,
                pendingSizeScale = sizeScaleOverride,
            )
            ClockEditDragTarget.Position -> copy(
                storedTopPaddingDp = topPaddingDp,
                editedTopPaddingDp = topPaddingDp,
                dragTarget = ClockEditDragTarget.None,
                pendingTopPaddingDp = topPaddingDp,
            )
            ClockEditDragTarget.None -> this
        }
    }

    fun resizeBy(deltaX: Float, deltaY: Float, density: Float, previewScale: Float): ClockEditModel {
        val x =
            when (alignment) {
                ClockEditAlignment.Right -> -deltaX
                ClockEditAlignment.Center -> deltaX * 2f
                ClockEditAlignment.Left -> deltaX
            }
        val dragDelta = if (abs(x) >= abs(deltaY)) x else deltaY
        val deltaDp = dragDelta / density / previewScale
        val currentScale = scaleGeometry.clampScale(editedSizeScale)
        val nextScale = scaleGeometry.resizeScale(currentScale, deltaDp)
        val frameWidthDelta = (nextScale - currentScale) * scaleGeometry.resizeDpPerScale
        val nextFrameWidth = scaleGeometry.frameWidthDp + frameWidthDelta
        val nextFrameHeight =
            scaleGeometry.frameHeightDp?.let {
                it * (nextFrameWidth / scaleGeometry.frameWidthDp.coerceAtLeast(1f))
            }
        return copy(
            editedSizeScale = nextScale,
            scaleGeometry = scaleGeometry.copy(
                requestedScale = nextScale,
                frameWidthDp = nextFrameWidth,
                frameHeightDp = nextFrameHeight,
            ),
        )
    }

    fun moveTopPaddingBy(deltaY: Float, density: Float, previewScale: Float): ClockEditModel {
        return copy(
            editedTopPaddingDp = topPaddingRange.clamp(
                editedTopPaddingDp + deltaY / density / previewScale,
            ),
        )
    }

    fun resetSize(): ClockEditModel = copy(
        storedSizeScale = scaleGeometry.clampScale(1f),
        editedSizeScale = scaleGeometry.clampScale(1f),
        dragTarget = ClockEditDragTarget.None,
        pendingSizeScale = scaleGeometry.clampScale(1f),
    )

    fun resetPosition(): ClockEditModel = copy(
        storedTopPaddingDp = topPaddingRange.clamp(0f),
        editedTopPaddingDp = topPaddingRange.clamp(0f),
        dragTarget = ClockEditDragTarget.None,
        pendingTopPaddingDp = topPaddingRange.clamp(0f),
    )

    companion object {
        fun from(
            storedSizeScale: Float,
            storedTopPaddingDp: Float,
            alignmentValue: String,
            scaleGeometry: ClockEditScaleGeometry =
                ClockEditScaleGeometry.default(
                    availableWidthDp = 0f,
                    requestedScale = storedSizeScale,
                    scaleRange = ClockSettingsRepository.sizeScaleRange,
                ),
            topPaddingRange: ClockTopPaddingRange =
                ClockTopPaddingRange(
                    CLOCK_EDIT_TOP_PADDING_MIN_DP,
                    ClockSettingsRepository.TOP_PADDING_MAX_DP,
                ),
        ): ClockEditModel {
            return ClockEditModel(
                storedSizeScale = storedSizeScale,
                editedSizeScale = scaleGeometry.clampScale(storedSizeScale),
                storedTopPaddingDp = storedTopPaddingDp,
                editedTopPaddingDp = topPaddingRange.clamp(storedTopPaddingDp),
                alignment = ClockEditAlignment.fromSetting(alignmentValue),
                scaleGeometry = scaleGeometry,
                topPaddingRange = topPaddingRange,
            )
        }
    }
}

internal data class ClockEditFrameBounds(
    val widthDp: Float,
    val heightDp: Float?,
) {
    fun widthIn(availableWidthDp: Float): Float {
        if (availableWidthDp <= 0f) return widthDp
        return widthDp.coerceIn(0f, availableWidthDp)
    }

    fun heightIn(availableHeightDp: Float): Float {
        if (availableHeightDp <= 0f) return heightDp?.coerceAtLeast(0f) ?: 0f
        return availableHeightDp
    }

    fun startOffsetDp(
        availableWidthDp: Float,
        alignment: ClockEditAlignment,
    ): Float {
        val width = widthIn(availableWidthDp)
        return when (alignment) {
            ClockEditAlignment.Left -> 0f
            ClockEditAlignment.Right -> availableWidthDp - width
            ClockEditAlignment.Center -> (availableWidthDp - width) / 2f
        }.coerceAtLeast(0f)
    }

    fun handleStartOffsetDp(
        availableWidthDp: Float,
        alignment: ClockEditAlignment,
    ): Float {
        val start = startOffsetDp(availableWidthDp, alignment)
        val width = widthIn(availableWidthDp)
        val offset = start + (width - CLOCK_EDIT_HANDLE_SIZE_DP).coerceAtLeast(0f)
        return offset.coerceIn(
            0f,
            (availableWidthDp - CLOCK_EDIT_HANDLE_SIZE_DP).coerceAtLeast(0f),
        )
    }

    fun topOffsetDp(availableHeightDp: Float): Float {
        val height = heightIn(availableHeightDp)
        if (availableHeightDp <= 0f) return 0f
        return ((availableHeightDp - height) / 2f)
            .coerceIn(0f, (availableHeightDp - height).coerceAtLeast(0f))
    }

    fun handleTopOffsetDp(availableHeightDp: Float): Float {
        val maxTop = (availableHeightDp - CLOCK_EDIT_HANDLE_SIZE_DP).coerceAtLeast(0f)
        val guideBottom = topOffsetDp(availableHeightDp) + heightIn(availableHeightDp)
        return (guideBottom - CLOCK_EDIT_HANDLE_SIZE_DP).coerceIn(0f, maxTop)
    }

    companion object {
        fun from(geometry: ClockEditScaleGeometry, availableWidthDp: Float): ClockEditFrameBounds =
            ClockEditFrameBounds(
                widthDp = geometry.frameWidthIn(availableWidthDp),
                heightDp = geometry.frameHeightDp,
            )
    }
}

internal enum class ClockEditAlignment {
    Left,
    Center,
    Right;

    companion object {
        fun fromSetting(value: String): ClockEditAlignment {
            return when (value) {
                ClockSettingsRepository.ALIGNMENT_LEFT -> Left
                ClockSettingsRepository.ALIGNMENT_RIGHT -> Right
                else -> Center
            }
        }
    }
}

internal enum class ClockEditDragTarget {
    None,
    Size,
    Position,
}

private fun Float.approximatelyEquals(other: Float): Boolean = abs(this - other) < 0.001f
