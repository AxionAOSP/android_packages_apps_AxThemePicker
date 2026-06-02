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

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.components.CommonBottomSheet
import com.android.axion.themepicker.ui.components.SheetDimens
import com.android.axion.compose.color.ColorPickerDialog
import com.android.systemui.shared.clocks.AxClockType
import com.android.systemui.shared.clocks.ClockSettingsRepository
import com.android.systemui.shared.clocks.view.AxClockView
import com.android.systemui.shared.clocks.view.BitmapDigitComposeClockView
import com.android.systemui.shared.clocks.view.BitmapFaceConfigs
import com.android.systemui.shared.clocks.view.OplusBigClockView
import com.android.systemui.shared.clocks.view.OplusClassicClockView
import com.android.systemui.shared.clocks.view.OplusGraffitiClockView
import com.android.systemui.shared.clocks.view.RenderMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val TileCorner = 20.dp
private val TileBorder = 2.dp
private val StyleTileWidth = 150.dp
private val StyleTileHeight = 100.dp
private val FaceTileWidth = 200.dp
private val FaceTileHeight = 150.dp
private const val STYLE_PREVIEW_SCALE = 0.35f
private const val FACE_PREVIEW_SCALE = 0.45f
private const val DEPTH_SETTINGS_KEY = "ax_depth_clock_enabled"
private const val DEPTH_ON = "on"
private const val DEPTH_OFF = "off"

@Composable
fun ClockFaceSheet(
    visible: Boolean,
    heightFraction: Float = 0.65f,
    onPreviewAnimationRequest: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allTypes = remember { AxClockType.entries }
    val pickerTypes = remember { AxClockType.pickerEntries }
    var currentClockId by remember { mutableStateOf("DEFAULT") }
    var currentAlignment by remember { mutableStateOf(ClockSettingsRepository.ALIGNMENT_CENTER) }
    var depthEnabled by remember { mutableStateOf(false) }
    var currentDatePosition by remember {
        mutableStateOf(ClockSettingsRepository.DATE_POSITION_ABOVE)
    }
    var currentInfoDisplayMode by remember {
        mutableStateOf(ClockSettingsRepository.INFO_DISPLAY_AUTO)
    }
    var currentInfoDisplaySources by remember {
        mutableStateOf(ClockSettingsRepository.DEFAULT_INFO_DISPLAY_SOURCES)
    }
    var currentClockColor by remember { mutableStateOf(ClockSettingsRepository.COLOR_AUTO) }
    var currentOplusClassicFace by remember {
        mutableStateOf(ClockSettingsRepository.OPLUS_CLASSIC_FACE_DEFAULT)
    }
    var currentOplusBigFace by remember {
        mutableStateOf(ClockSettingsRepository.OPLUS_BIG_FACE_DEFAULT)
    }
    var currentOplusBigDualTone by remember { mutableStateOf(false) }
    var currentOplusGraffitiFace by remember {
        mutableStateOf(ClockSettingsRepository.OPLUS_GRAFFITI_FACE_DEFAULT)
    }
    var currentOplusGraffitiAngle by remember {
        mutableStateOf(ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_CENTER)
    }
    var showColorPicker by remember { mutableStateOf(false) }
    var isLiveWallpaper by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val id = readCurrentClockId(context)
            val align = readAlignment(context)
            val depth = readDepthEnabled(context)
            val datePos = readDatePosition(context)
            val infoDisplayMode = ClockSettingsRepository.readInfoDisplayMode(context)
            val infoDisplaySources = ClockSettingsRepository.readInfoDisplaySources(context)
            val clockColor = readClockColor(context)
            val classicFace = readOplusClassicFace(context)
            val bigFace = readOplusBigFace(context)
            val bigDualTone = readOplusBigDualTone(context)
            val graffitiFace = readOplusGraffitiFace(context)
            val graffitiAngle = readOplusGraffitiAngle(context)
            val liveWp = WallpaperManager.getInstance(context).wallpaperInfo != null
            withContext(Dispatchers.Main) {
                currentClockId = id
                currentAlignment = align
                depthEnabled = depth
                currentDatePosition = datePos
                currentInfoDisplayMode = infoDisplayMode
                currentInfoDisplaySources = infoDisplaySources
                currentClockColor = clockColor
                currentOplusClassicFace = classicFace
                currentOplusBigFace = bigFace
                currentOplusBigDualTone = bigDualTone
                currentOplusGraffitiFace = graffitiFace
                currentOplusGraffitiAngle = graffitiAngle
                isLiveWallpaper = liveWp
            }
        }
    }

    val selectedType =
        remember(currentClockId) {
            allTypes.firstOrNull { context.resources.getString(it.clockId) == currentClockId }
                ?: AxClockType.NTYPE
        }
    val isNoClock = selectedType == AxClockType.NONE

    val isDigitFamily =
        remember(selectedType) {
            val style = selectedType.bitmapFaceStyle ?: return@remember false
            val config = BitmapFaceConfigs.getConfig(style) ?: return@remember false
            config.renderMode !is RenderMode.AnalogClock
        }
    val hasDateSupport = !isNoClock && (selectedType.bitmapFaceStyle != null || selectedType.isOplusClock)
    val supportsInfoDisplay = !isNoClock
    val supportsColorOverride = !isNoClock && selectedType != AxClockType.CYBERPUNK
    val supportsOplusClassicFace = selectedType == AxClockType.OPLUS_CLASSIC
    val supportsOplusBigFace = selectedType == AxClockType.OPLUS_BIG
    val supportsOplusGraffitiFace = selectedType == AxClockType.OPLUS_PLAYFUL
    val clockPreviewSettingsKey =
        "$currentAlignment:$currentDatePosition:$currentClockColor:" +
            "$currentOplusClassicFace:$currentOplusBigFace:$currentOplusBigDualTone:" +
            "$currentOplusGraffitiFace:$currentOplusGraffitiAngle"
    val digitFaceTypes = remember {
        pickerTypes.filter { type ->
            val style = type.bitmapFaceStyle ?: return@filter false
            val config = BitmapFaceConfigs.getConfig(style) ?: return@filter false
            config.renderMode !is RenderMode.AnalogClock
        }
    }
    val primaryTypes = remember {
        buildList {
            add(AxClockType.NONE)
            add(AxClockType.NTYPE)
            addAll(
                pickerTypes.filter {
                    it != AxClockType.NONE &&
                        it != AxClockType.NTYPE &&
                        (it.bitmapFaceStyle == null || it == AxClockType.GRAPHIC)
                }
            )
        }
    }

    fun writeClockId(type: AxClockType) {
        onPreviewAnimationRequest()
        val clockId = context.resources.getString(type.clockId)
        currentClockId = clockId
        val json =
            JSONObject()
                .apply {
                    put("clockId", clockId)
                    put(
                        "metadata",
                        JSONObject().apply { put("appliedTimestamp", System.currentTimeMillis()) },
                    )
                    put("axes", JSONArray())
                }
                .toString()
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_CLOCK_FACE,
                json,
            )
        }
    }

    fun writeAlignment(value: String) {
        onPreviewAnimationRequest()
        currentAlignment = value
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_ALIGNMENT,
                value,
            )
        }
    }

    fun writeDepth(enabled: Boolean) {
        onPreviewAnimationRequest()
        depthEnabled = enabled
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putInt(
                context.contentResolver,
                DEPTH_SETTINGS_KEY,
                if (enabled) 1 else 0,
            )
        }
    }

    fun writeDatePosition(value: String) {
        onPreviewAnimationRequest()
        currentDatePosition = value
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_DATE_POSITION,
                value,
            )
        }
    }

    fun writeInfoDisplayMode(value: String) {
        onPreviewAnimationRequest()
        currentInfoDisplayMode = value
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_INFO_DISPLAY_MODE,
                value,
            )
        }
    }

    fun writeInfoDisplaySource(value: String) {
        onPreviewAnimationRequest()
        val updated = if (value in currentInfoDisplaySources) {
            currentInfoDisplaySources - value
        } else {
            currentInfoDisplaySources + value
        }
        currentInfoDisplaySources = updated
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_INFO_DISPLAY_SOURCES,
                updated.sorted().joinToString(","),
            )
        }
    }

    fun writeClockColor(value: String) {
        onPreviewAnimationRequest()
        currentClockColor = value
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_CLOCK_COLOR,
                value,
            )
        }
    }

    fun writeOplusClassicFace(value: String) {
        onPreviewAnimationRequest()
        currentOplusClassicFace = value
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_OPLUS_CLASSIC_FACE,
                value,
            )
        }
    }

    fun writeOplusBigFace(value: String) {
        onPreviewAnimationRequest()
        currentOplusBigFace = value
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_OPLUS_BIG_FACE,
                value,
            )
        }
    }

    fun writeOplusBigDualTone(enabled: Boolean) {
        onPreviewAnimationRequest()
        currentOplusBigDualTone = enabled
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putInt(
                context.contentResolver,
                ClockSettingsRepository.SETTING_OPLUS_BIG_DUAL_TONE,
                if (enabled) 1 else 0,
            )
        }
    }

    fun writeOplusGraffitiFace(value: String) {
        onPreviewAnimationRequest()
        currentOplusGraffitiFace = value
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_OPLUS_GRAFFITI_FACE,
                value,
            )
        }
    }

    fun writeOplusGraffitiAngle(value: String) {
        onPreviewAnimationRequest()
        currentOplusGraffitiAngle = value
        scope.launch(Dispatchers.IO) {
            Settings.Secure.putString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_OPLUS_GRAFFITI_ANGLE,
                value,
            )
        }
    }

    CommonBottomSheet(
        visible = visible,
        title = stringResource(R.string.clock_face),
        heightFraction = heightFraction,
        surfaceColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimAlpha = 0f,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SheetDimens.SheetPagerPadding)
        ) {
            SectionTitle(stringResource(R.string.clock_style))
            Spacer(modifier = Modifier.height(8.dp))
            ClockStyleGrid(
                context = context,
                primaryTypes = primaryTypes,
                selectedType = selectedType,
                tileWidth = StyleTileWidth,
                tileHeight = StyleTileHeight,
                previewScale = STYLE_PREVIEW_SCALE,
                settingsKey = clockPreviewSettingsKey,
                onSelect = { writeClockId(it) },
                isSelectedOverride = { type ->
                    if (type == AxClockType.NTYPE) isDigitFamily else type == selectedType
                },
            )

            if (isDigitFamily && digitFaceTypes.size > 1) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.clock_face_style))
                Spacer(modifier = Modifier.height(8.dp))
                ClockStyleGrid(
                    context = context,
                    primaryTypes = digitFaceTypes,
                    selectedType = selectedType,
                    tileWidth = FaceTileWidth,
                    tileHeight = FaceTileHeight,
                    previewScale = FACE_PREVIEW_SCALE,
                    settingsKey = clockPreviewSettingsKey,
                    onSelect = { writeClockId(it) },
                )
            }

            if (supportsOplusClassicFace) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.clock_face_style))
                Spacer(modifier = Modifier.height(8.dp))
                OplusClassicFaceRow(
                    context = context,
                    selected = currentOplusClassicFace,
                    settingsKey = clockPreviewSettingsKey,
                    onSelect = { writeOplusClassicFace(it) },
                )
            }

            if (supportsOplusBigFace) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.clock_face_style))
                Spacer(modifier = Modifier.height(8.dp))
                OplusBigFaceRow(
                    context = context,
                    selected = currentOplusBigFace,
                    dualTone = currentOplusBigDualTone,
                    settingsKey = clockPreviewSettingsKey,
                    onSelect = { writeOplusBigFace(it) },
                )
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.clock_dual_tone))
                Spacer(modifier = Modifier.height(8.dp))
                OptionRow(
                    options =
                        listOf(
                            OptionItem(DEPTH_OFF, stringResource(R.string.off)) {
                                DualToneOffIcon(it)
                            },
                            OptionItem(DEPTH_ON, stringResource(R.string.on)) {
                                DualToneOnIcon(it)
                            },
                        ),
                    selected = if (currentOplusBigDualTone) DEPTH_ON else DEPTH_OFF,
                    onSelect = { writeOplusBigDualTone(it == DEPTH_ON) },
                )
            }

            if (supportsOplusGraffitiFace) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.clock_face_style))
                Spacer(modifier = Modifier.height(8.dp))
                OplusGraffitiFaceRow(
                    context = context,
                    selected = currentOplusGraffitiFace,
                    angle = currentOplusGraffitiAngle,
                    settingsKey = clockPreviewSettingsKey,
                    onSelect = { writeOplusGraffitiFace(it) },
                )
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.clock_graffiti_angle))
                Spacer(modifier = Modifier.height(8.dp))
                OptionRow(
                    options =
                        listOf(
                            OptionItem(
                                ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_LEFT,
                                stringResource(R.string.clock_align_left),
                            ) {
                                GraffitiAngleLeftIcon(it)
                            },
                            OptionItem(
                                ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_CENTER,
                                stringResource(R.string.clock_align_center),
                            ) {
                                GraffitiAngleCenterIcon(it)
                            },
                            OptionItem(
                                ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_RIGHT,
                                stringResource(R.string.clock_align_right),
                            ) {
                                GraffitiAngleRightIcon(it)
                            },
                        ),
                    selected = currentOplusGraffitiAngle,
                    onSelect = { writeOplusGraffitiAngle(it) },
                )
            }

            if (!isNoClock) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.clock_alignment))
                Spacer(modifier = Modifier.height(8.dp))
                OptionRow(
                    options =
                        listOf(
                            OptionItem(
                                ClockSettingsRepository.ALIGNMENT_LEFT,
                                stringResource(R.string.clock_align_left),
                            ) {
                                AlignLeftIcon(it)
                            },
                            OptionItem(
                                ClockSettingsRepository.ALIGNMENT_CENTER,
                                stringResource(R.string.clock_align_center),
                            ) {
                                AlignCenterIcon(it)
                            },
                            OptionItem(
                                ClockSettingsRepository.ALIGNMENT_RIGHT,
                                stringResource(R.string.clock_align_right),
                            ) {
                                AlignRightIcon(it)
                            },
                        ),
                    selected = currentAlignment,
                    onSelect = { writeAlignment(it) },
                )

                if (hasDateSupport) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionTitle(stringResource(R.string.clock_date_position))
                    Spacer(modifier = Modifier.height(8.dp))
                    OptionRow(
                        options =
                            listOf(
                                OptionItem(
                                    ClockSettingsRepository.DATE_POSITION_ABOVE,
                                    stringResource(R.string.clock_date_above),
                                ) {
                                    DateAboveIcon(it)
                                },
                                OptionItem(
                                    ClockSettingsRepository.DATE_POSITION_BELOW,
                                    stringResource(R.string.clock_date_below),
                                ) {
                                    DateBelowIcon(it)
                                },
                            ),
                        selected = currentDatePosition,
                        onSelect = { writeDatePosition(it) },
                    )
                }

                if (supportsInfoDisplay) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionTitle(stringResource(R.string.clock_info_display))
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoDisplayRow(
                        options =
                            listOf(
                                OptionItem(
                                    ClockSettingsRepository.INFO_DISPLAY_AUTO,
                                    stringResource(R.string.clock_info_auto),
                                ) {
                                    InfoAutoIcon(it)
                                },
                                OptionItem(
                                    ClockSettingsRepository.INFO_DISPLAY_DATE,
                                    stringResource(R.string.clock_info_date),
                                ) {
                                    InfoDateIcon(it)
                                },
                                OptionItem(
                                    ClockSettingsRepository.INFO_DISPLAY_OFF,
                                    stringResource(R.string.off),
                                ) {
                                    InfoOffIcon(it)
                                },
                            ),
                        selected = currentInfoDisplayMode,
                        onSelect = { writeInfoDisplayMode(it) },
                    )
                    if (currentInfoDisplayMode == ClockSettingsRepository.INFO_DISPLAY_AUTO) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.clock_info_sources),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoSourceRow(
                            options =
                                listOf(
                                    OptionItem(
                                        ClockSettingsRepository.INFO_DISPLAY_MEDIA,
                                        stringResource(R.string.clock_info_media),
                                    ) {
                                        InfoMediaIcon(it)
                                    },
                                    OptionItem(
                                        ClockSettingsRepository.INFO_DISPLAY_SMARTSPACE,
                                        stringResource(R.string.clock_info_smartspace),
                                    ) {
                                        InfoSmartspaceIcon(it)
                                    },
                                    OptionItem(
                                        ClockSettingsRepository.INFO_DISPLAY_ALARM,
                                        stringResource(R.string.clock_info_alarm),
                                    ) {
                                        InfoAlarmIcon(it)
                                    },
                                    OptionItem(
                                        ClockSettingsRepository.INFO_DISPLAY_CALENDAR,
                                        stringResource(R.string.clock_info_calendar),
                                    ) {
                                        InfoCalendarIcon(it)
                                    },
                                    OptionItem(
                                        ClockSettingsRepository.INFO_DISPLAY_WEATHER,
                                        stringResource(R.string.clock_info_weather),
                                    ) {
                                        InfoWeatherIcon(it)
                                    },
                                ),
                            selected = currentInfoDisplaySources,
                            onToggle = { writeInfoDisplaySource(it) },
                        )
                    }
                }

                if (supportsColorOverride) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionTitle(stringResource(R.string.clock_color))
                    Spacer(modifier = Modifier.height(8.dp))
                    ClockColorRow(
                        selected = currentClockColor,
                        onSelect = { writeClockColor(it) },
                        onCustom = { showColorPicker = true },
                    )
                }

                if (!isLiveWallpaper) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionTitle(stringResource(R.string.depth_effect))
                    Spacer(modifier = Modifier.height(8.dp))
                    OptionRow(
                        options =
                            listOf(
                                OptionItem(DEPTH_OFF, stringResource(R.string.off)) {
                                    DepthOffIcon(it)
                                },
                                OptionItem(DEPTH_ON, stringResource(R.string.on)) {
                                    DepthOnIcon(it)
                                },
                            ),
                        selected = if (depthEnabled) DEPTH_ON else DEPTH_OFF,
                        onSelect = { writeDepth(it == DEPTH_ON) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(SheetDimens.SheetPagerSpacingNav))
        }
    }

    if (showColorPicker) {
        val initialColor =
            try {
                if (currentClockColor != ClockSettingsRepository.COLOR_AUTO) {
                    Color(AndroidColor.parseColor(currentClockColor))
                } else Color.White
            } catch (_: Exception) {
                Color.White
            }

        ColorPickerDialog(
            initialColor = initialColor,
            title = stringResource(R.string.clock_color),
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                val hex = "#%08X".format(color.toArgb())
                writeClockColor(hex)
                showColorPicker = false
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ClockTileRow(
    itemCount: Int,
    selectedIndex: Int,
    tileWidth: Dp,
    content: @Composable RowScope.(Int) -> Unit,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val tileWidthPx = with(density) { tileWidth.toPx() }
    val spacingPx = with(density) { 12.dp.toPx() }

    LaunchedEffect(selectedIndex, tileWidthPx, spacingPx) {
        val targetPx = (selectedIndex * (tileWidthPx + spacingPx)).toInt()
        scrollState.animateScrollTo(targetPx)
    }

    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(itemCount) { index ->
            content(index)
        }
    }
}

@Composable
private fun OplusClassicFaceRow(
    context: Context,
    selected: String,
    settingsKey: String,
    onSelect: (String) -> Unit,
) {
    val selectedIndex =
        remember(selected) {
            OPLUS_CLASSIC_FACE_OPTIONS
                .indexOfFirst { face -> face == selected }
                .coerceAtLeast(0)
        }

    ClockTileRow(
        itemCount = OPLUS_CLASSIC_FACE_OPTIONS.size,
        selectedIndex = selectedIndex,
        tileWidth = FaceTileWidth,
    ) { index ->
        val face = OPLUS_CLASSIC_FACE_OPTIONS[index]
        val isSelected = face == selected
        ClockTile(
            context = context,
            type = AxClockType.OPLUS_CLASSIC,
            isSelected = isSelected,
            previewScale = FACE_PREVIEW_SCALE,
            settingsKey = "$settingsKey:$face",
            onClick = { onSelect(face) },
            modifier = Modifier.width(FaceTileWidth).height(FaceTileHeight),
            configureView = { view ->
                (view as? OplusClassicClockView)?.previewClassicFace = face
            },
        )
    }
}

@Composable
private fun OplusBigFaceRow(
    context: Context,
    selected: String,
    dualTone: Boolean,
    settingsKey: String,
    onSelect: (String) -> Unit,
) {
    val selectedIndex =
        remember(selected) {
            OPLUS_BIG_FACE_OPTIONS
                .indexOfFirst { face -> face == selected }
                .coerceAtLeast(0)
        }

    ClockTileRow(
        itemCount = OPLUS_BIG_FACE_OPTIONS.size,
        selectedIndex = selectedIndex,
        tileWidth = FaceTileWidth,
    ) { index ->
        val face = OPLUS_BIG_FACE_OPTIONS[index]
        val isSelected = face == selected
        ClockTile(
            context = context,
            type = AxClockType.OPLUS_BIG,
            isSelected = isSelected,
            previewScale = FACE_PREVIEW_SCALE,
            settingsKey = "$settingsKey:$face",
            onClick = { onSelect(face) },
            modifier = Modifier.width(FaceTileWidth).height(FaceTileHeight),
            configureView = { view ->
                (view as? OplusBigClockView)?.let { bigView ->
                    bigView.previewBigFace = face
                    bigView.previewBigDualTone = dualTone
                }
            },
        )
    }
}

@Composable
private fun OplusGraffitiFaceRow(
    context: Context,
    selected: String,
    angle: String,
    settingsKey: String,
    onSelect: (String) -> Unit,
) {
    val selectedIndex =
        remember(selected) {
            OPLUS_GRAFFITI_FACE_OPTIONS
                .indexOfFirst { face -> face == selected }
                .coerceAtLeast(0)
        }

    ClockTileRow(
        itemCount = OPLUS_GRAFFITI_FACE_OPTIONS.size,
        selectedIndex = selectedIndex,
        tileWidth = FaceTileWidth,
    ) { index ->
        val face = OPLUS_GRAFFITI_FACE_OPTIONS[index]
        val isSelected = face == selected
        ClockTile(
            context = context,
            type = AxClockType.OPLUS_PLAYFUL,
            isSelected = isSelected,
            previewScale = FACE_PREVIEW_SCALE,
            settingsKey = "$settingsKey:$face",
            onClick = { onSelect(face) },
            modifier = Modifier.width(FaceTileWidth).height(FaceTileHeight),
            configureView = { view ->
                (view as? OplusGraffitiClockView)?.let { graffitiView ->
                    graffitiView.previewGraffitiFace = face
                    graffitiView.previewGraffitiAngle = angle
                }
            },
        )
    }
}

@Composable
private fun ClockStyleGrid(
    context: Context,
    primaryTypes: List<AxClockType>,
    selectedType: AxClockType,
    tileWidth: Dp,
    tileHeight: Dp,
    previewScale: Float,
    settingsKey: String,
    onSelect: (AxClockType) -> Unit,
    isSelectedOverride: ((AxClockType) -> Boolean)? = null,
) {
    val selectedIndex =
        remember(selectedType, primaryTypes) {
            primaryTypes
                .indexOfFirst { type -> isSelectedOverride?.invoke(type) ?: (type == selectedType) }
                .coerceAtLeast(0)
        }

    ClockTileRow(
        itemCount = primaryTypes.size,
        selectedIndex = selectedIndex,
        tileWidth = tileWidth,
    ) { index ->
        val type = primaryTypes[index]
        val isSelected = isSelectedOverride?.invoke(type) ?: (type == selectedType)
        ClockTile(
            context = context,
            type = type,
            isSelected = isSelected,
            previewScale = previewScale,
            settingsKey = settingsKey,
            onClick = { onSelect(type) },
            modifier = Modifier.width(tileWidth).height(tileHeight),
        )
    }
}

@Composable
private fun ClockTile(
    context: Context,
    type: AxClockType,
    isSelected: Boolean,
    previewScale: Float,
    settingsKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    configureView: (AxClockView) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = isSystemInDarkTheme()

    val borderColor = if (isSelected) colors.primary else Color.Transparent
    val bgColor = colors.surfaceBright

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(TileCorner))
                .background(bgColor)
                .border(TileBorder, borderColor, RoundedCornerShape(TileCorner))
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        key(type, settingsKey) {
            if (type == AxClockType.NONE) {
                Text(
                    text = stringResource(R.string.none),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            } else {
                val clockView = remember {
                    val inflater = LayoutInflater.from(context)
                    val view = inflater.inflate(type.viewId, null) as AxClockView
                    (view as? BitmapDigitComposeClockView)?.let { bitmapView ->
                        type.bitmapFaceStyle?.let { bitmapView.faceStyle = it }
                    }
                    configureView(view)
                    view.setupPreview()
                    view.onRegionDarknessChanged(isDarkTheme)
                    view
                }

                DisposableEffect(Unit) {
                    onDispose { (clockView.parent as? ViewGroup)?.removeView(clockView) }
                }

                AndroidView(
                    factory = {
                        (clockView.parent as? ViewGroup)?.removeView(clockView)
                        clockView.layoutParams =
                            FrameLayout.LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT,
                            )
                        FrameLayout(it).apply {
                            layoutParams =
                                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                            addView(clockView)
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth().wrapContentHeight().scaledLayout(previewScale),
                )
            }
        }
    }
}

private class OptionItem(
    val value: String,
    val label: String,
    val icon: @Composable (Color) -> Unit,
)

private val OPLUS_CLASSIC_FACE_OPTIONS = listOf(
    ClockSettingsRepository.OPLUS_CLASSIC_FACE_DEFAULT,
    ClockSettingsRepository.OPLUS_CLASSIC_FACE_STACKED,
)

private val OPLUS_BIG_FACE_OPTIONS = listOf(
    ClockSettingsRepository.OPLUS_BIG_FACE_DEFAULT,
    ClockSettingsRepository.OPLUS_BIG_FACE_WIDE,
)

private val OPLUS_GRAFFITI_FACE_OPTIONS = listOf(
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_SANS,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_DEFAULT,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_BRIGHT,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_CUTE,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_DIGIT04,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_GAME,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_KEEP,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_WENDAO,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_SHENQI,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_GALADA,
    ClockSettingsRepository.OPLUS_GRAFFITI_FACE_MODAK,
)

@Composable
private fun OptionRow(options: List<OptionItem>, selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val borderColor = if (isSelected) colors.primary else Color.Transparent
            val bgColor = colors.surfaceBright
            val iconTint = if (isSelected) colors.primary else colors.onSurfaceVariant

            Box(
                modifier =
                    Modifier.weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(TileCorner))
                        .background(bgColor)
                        .border(TileBorder, borderColor, RoundedCornerShape(TileCorner))
                        .clickable { onSelect(option.value) },
                contentAlignment = Alignment.Center,
            ) {
                option.icon(iconTint)
            }
        }
    }
}

@Composable
private fun InfoDisplayRow(options: List<OptionItem>, selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)

    LaunchedEffect(selectedIndex) {
        val itemWidth = with(density) { 112.dp.toPx() }
        val spacing = with(density) { 10.dp.toPx() }
        scrollState.animateScrollTo((selectedIndex * (itemWidth + spacing)).toInt())
    }

    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val contentColor = if (isSelected) colors.primary else colors.onSurfaceVariant

            InfoOptionTile(
                option = option,
                selected = isSelected,
                containerColor = colors.surfaceBright,
                contentColor = contentColor,
                onClick = { onSelect(option.value) },
            )
        }
    }
}

@Composable
private fun InfoSourceRow(options: List<OptionItem>, selected: Set<String>, onToggle: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value in selected
            val containerColor = if (isSelected) colors.secondaryContainer else colors.surfaceBright
            val contentColor = if (isSelected) colors.onSecondaryContainer else colors.onSurfaceVariant

            InfoOptionTile(
                option = option,
                selected = isSelected,
                containerColor = containerColor,
                contentColor = contentColor,
                onClick = { onToggle(option.value) },
            )
        }
    }
}

@Composable
private fun InfoOptionTile(
    option: OptionItem,
    selected: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        modifier =
            Modifier.width(112.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(TileCorner))
                .background(containerColor)
                .border(TileBorder, borderColor, RoundedCornerShape(TileCorner))
                .clickable { onClick() }
                .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        option.icon(contentColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class ClockColorOption(val value: String, val color: Color?)

private val CLOCK_COLOR_PRESETS =
    listOf(
        ClockColorOption(ClockSettingsRepository.COLOR_AUTO, null),
        ClockColorOption("#FFFFFFFF", Color.White),
        ClockColorOption("#FF000000", Color.Black),
        ClockColorOption("#FFFF453A", Color(0xFFFF453A)),
        ClockColorOption("#FFFF9F0A", Color(0xFFFF9F0A)),
        ClockColorOption("#FFFFD60A", Color(0xFFFFD60A)),
        ClockColorOption("#FF34C759", Color(0xFF34C759)),
        ClockColorOption("#FF0A84FF", Color(0xFF0A84FF)),
        ClockColorOption("#FF5856D6", Color(0xFF5856D6)),
        ClockColorOption("#FFBF5AF2", Color(0xFFBF5AF2)),
    )

@Composable
private fun ClockColorRow(selected: String, onSelect: (String) -> Unit, onCustom: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val isCustom =
        selected != ClockSettingsRepository.COLOR_AUTO &&
            CLOCK_COLOR_PRESETS.none { it.value.equals(selected, ignoreCase = true) }

    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CLOCK_COLOR_PRESETS.forEach { option ->
            val isSelected =
                option.value.equals(selected, ignoreCase = true) ||
                    (option.value == ClockSettingsRepository.COLOR_AUTO &&
                        selected == ClockSettingsRepository.COLOR_AUTO)

            ColorSwatch(
                color = option.color,
                isSelected = isSelected,
                isAuto = option.color == null,
                onClick = { onSelect(option.value) },
            )
        }

        ColorSwatch(
            color =
                if (isCustom) {
                    try {
                        Color(AndroidColor.parseColor(selected))
                    } catch (_: Exception) {
                        null
                    }
                } else null,
            isSelected = isCustom,
            isAuto = false,
            isCustomSwatch = true,
            onClick = onCustom,
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color?,
    isSelected: Boolean,
    isAuto: Boolean,
    isCustomSwatch: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val swatchSize = 40.dp
    val shape = CircleShape

    Box(
        modifier =
            Modifier.size(swatchSize)
                .clip(shape)
                .then(if (isSelected) Modifier.border(2.5.dp, colors.primary, shape) else Modifier)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        val innerSize = if (isSelected) swatchSize - 6.dp else swatchSize

        when {
            isAuto -> {
                Canvas(modifier = Modifier.size(innerSize).clip(shape)) {
                    val half = size.width / 2f
                    drawCircle(Color.White, radius = half)
                    drawArc(Color.Black, startAngle = 90f, sweepAngle = 180f, useCenter = true)
                }
            }
            isCustomSwatch && color == null -> {
                Canvas(modifier = Modifier.size(innerSize).clip(shape)) {
                    val r = size.width / 2f
                    val segments = 6
                    val hueColors =
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red,
                        )
                    val sweep = 360f / segments
                    hueColors.dropLast(1).forEachIndexed { i, c ->
                        drawArc(
                            c,
                            startAngle = i * sweep - 90f,
                            sweepAngle = sweep + 1f,
                            useCenter = true,
                        )
                    }
                }
            }
            color != null -> {
                val needsBorder = color == Color.White || color == Color.Black
                Canvas(
                    modifier =
                        Modifier.size(innerSize)
                            .clip(shape)
                            .then(
                                if (needsBorder && !isSelected)
                                    Modifier.border(1.dp, colors.outlineVariant, shape)
                                else Modifier
                            )
                ) {
                    drawCircle(color)
                }
            }
        }
    }
}

@Composable
private fun OptionIcon(tint: Color, draw: DrawScope.(Color) -> Unit) {
    Canvas(modifier = Modifier.size(28.dp)) { draw(tint) }
}

@Composable
private fun InfoAutoIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.2f.dp.toPx()
        val cx = size.width / 2f
        val ys = listOf(size.height * 0.28f, size.height * 0.5f, size.height * 0.72f)
        ys.forEachIndexed { index, y ->
            val width = size.width * (0.56f - index * 0.08f)
            drawLine(color.copy(alpha = 1f - index * 0.18f), Offset(cx - width / 2f, y), Offset(cx + width / 2f, y), sw, StrokeCap.Round)
        }
    }
}

@Composable
private fun InfoDateIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.dp.toPx()
        val left = 6.dp.toPx()
        val right = size.width - left
        val top = 7.dp.toPx()
        val bottom = size.height - 5.dp.toPx()
        drawLine(color, Offset(left, top), Offset(right, top), sw, StrokeCap.Round)
        drawLine(color, Offset(left, bottom), Offset(right, bottom), sw, StrokeCap.Round)
        drawLine(color, Offset(left, top), Offset(left, bottom), sw, StrokeCap.Round)
        drawLine(color, Offset(right, top), Offset(right, bottom), sw, StrokeCap.Round)
        drawLine(color.copy(alpha = 0.65f), Offset(left + 4.dp.toPx(), size.height * 0.52f), Offset(right - 4.dp.toPx(), size.height * 0.52f), sw, StrokeCap.Round)
    }
}

@Composable
private fun InfoOffIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.4f.dp.toPx()
        val left = 7.dp.toPx()
        val right = size.width - left
        val y1 = size.height * 0.38f
        val y2 = size.height * 0.62f
        drawLine(color.copy(alpha = 0.45f), Offset(left, y1), Offset(right, y1), sw, StrokeCap.Round)
        drawLine(color.copy(alpha = 0.45f), Offset(left + 3.dp.toPx(), y2), Offset(right - 3.dp.toPx(), y2), sw, StrokeCap.Round)
        drawLine(color, Offset(left, size.height - 6.dp.toPx()), Offset(right, 6.dp.toPx()), sw, StrokeCap.Round)
    }
}

@Composable
private fun DualToneOffIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 3.dp.toPx()
        val left = 5.dp.toPx()
        val right = size.width - left
        val top = size.height * 0.38f
        val bottom = size.height * 0.62f
        drawLine(color, Offset(left, top), Offset(right, top), sw, StrokeCap.Round)
        drawLine(color, Offset(left, bottom), Offset(right, bottom), sw, StrokeCap.Round)
    }
}

@Composable
private fun DualToneOnIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 3.dp.toPx()
        val left = 5.dp.toPx()
        val right = size.width - left
        val top = size.height * 0.38f
        val bottom = size.height * 0.62f
        drawLine(color, Offset(left, top), Offset(right, top), sw, StrokeCap.Round)
        drawLine(color.copy(alpha = 0.48f), Offset(left, bottom), Offset(right, bottom), sw, StrokeCap.Round)
    }
}

@Composable
private fun InfoMediaIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.2f.dp.toPx()
        val stemX = size.width * 0.58f
        val stemTop = 6.dp.toPx()
        val stemBottom = size.height * 0.62f
        drawLine(color, Offset(stemX, stemTop), Offset(stemX, stemBottom), sw, StrokeCap.Round)
        drawLine(color, Offset(stemX, stemTop), Offset(size.width * 0.75f, stemTop + 3.dp.toPx()), sw, StrokeCap.Round)
        drawCircle(color, radius = 4.dp.toPx(), center = Offset(size.width * 0.42f, size.height * 0.72f))
        drawLine(color, Offset(stemX, stemBottom), Offset(size.width * 0.42f, size.height * 0.72f), sw, StrokeCap.Round)
    }
}

@Composable
private fun InfoSmartspaceIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(color, Offset(cx, 5.dp.toPx()), Offset(cx, size.height - 5.dp.toPx()), sw, StrokeCap.Round)
        drawLine(color, Offset(5.dp.toPx(), cy), Offset(size.width - 5.dp.toPx(), cy), sw, StrokeCap.Round)
        drawLine(color.copy(alpha = 0.62f), Offset(8.dp.toPx(), 8.dp.toPx()), Offset(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()), sw, StrokeCap.Round)
        drawLine(color.copy(alpha = 0.62f), Offset(8.dp.toPx(), size.height - 8.dp.toPx()), Offset(size.width - 8.dp.toPx(), 8.dp.toPx()), sw, StrokeCap.Round)
    }
}

@Composable
private fun InfoAlarmIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.dp.toPx()
        val center = Offset(size.width / 2f, size.height * 0.56f)
        drawCircle(color, radius = 8.dp.toPx(), center = center, style = Stroke(sw))
        drawLine(color, center, Offset(center.x, center.y - 5.dp.toPx()), sw, StrokeCap.Round)
        drawLine(color, center, Offset(center.x + 4.dp.toPx(), center.y + 2.dp.toPx()), sw, StrokeCap.Round)
        drawLine(color, Offset(8.dp.toPx(), 6.dp.toPx()), Offset(4.dp.toPx(), 10.dp.toPx()), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width - 8.dp.toPx(), 6.dp.toPx()), Offset(size.width - 4.dp.toPx(), 10.dp.toPx()), sw, StrokeCap.Round)
    }
}

@Composable
private fun InfoCalendarIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.dp.toPx()
        val left = 5.dp.toPx()
        val right = size.width - left
        val top = 7.dp.toPx()
        val bottom = size.height - 5.dp.toPx()
        drawLine(color, Offset(left, top), Offset(right, top), sw, StrokeCap.Round)
        drawLine(color, Offset(left, bottom), Offset(right, bottom), sw, StrokeCap.Round)
        drawLine(color, Offset(left, top), Offset(left, bottom), sw, StrokeCap.Round)
        drawLine(color, Offset(right, top), Offset(right, bottom), sw, StrokeCap.Round)
        drawLine(color, Offset(left, top + 6.dp.toPx()), Offset(right, top + 6.dp.toPx()), sw, StrokeCap.Round)
        drawCircle(color, radius = 1.8f.dp.toPx(), center = Offset(size.width * 0.38f, size.height * 0.62f))
        drawCircle(color, radius = 1.8f.dp.toPx(), center = Offset(size.width * 0.62f, size.height * 0.62f))
    }
}

@Composable
private fun InfoWeatherIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.2f.dp.toPx()
        drawCircle(color.copy(alpha = 0.72f), radius = 4.5f.dp.toPx(), center = Offset(size.width * 0.68f, size.height * 0.32f))
        drawArc(color, startAngle = 200f, sweepAngle = 250f, useCenter = false, topLeft = Offset(6.dp.toPx(), 10.dp.toPx()), size = Size(16.dp.toPx(), 12.dp.toPx()), style = Stroke(sw, cap = StrokeCap.Round))
        drawLine(color, Offset(7.dp.toPx(), size.height * 0.68f), Offset(size.width - 6.dp.toPx(), size.height * 0.68f), sw, StrokeCap.Round)
    }
}

@Composable
private fun AlignLeftIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.5f.dp.toPx()
        val x = 4.dp.toPx()
        val gap = 4.dp.toPx()
        val maxW = size.width - x * 2
        val startY = (size.height - sw * 2 - gap) / 2f

        drawLine(
            color,
            Offset(x, startY + sw / 2f),
            Offset(x + maxW * 0.75f, startY + sw / 2f),
            sw,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(x, startY + sw + gap + sw / 2f),
            Offset(x + maxW * 0.5f, startY + sw + gap + sw / 2f),
            sw,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun AlignRightIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.5f.dp.toPx()
        val x = 4.dp.toPx()
        val gap = 4.dp.toPx()
        val maxW = size.width - x * 2
        val startY = (size.height - sw * 2 - gap) / 2f
        val endX = size.width - x

        drawLine(
            color,
            Offset(endX - maxW * 0.75f, startY + sw / 2f),
            Offset(endX, startY + sw / 2f),
            sw,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(endX - maxW * 0.5f, startY + sw + gap + sw / 2f),
            Offset(endX, startY + sw + gap + sw / 2f),
            sw,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun AlignCenterIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.5f.dp.toPx()
        val cx = size.width / 2f
        val inset = 4.dp.toPx()
        val maxW = size.width - inset * 2
        val gap = 4.dp.toPx()
        val startY = (size.height - sw * 2 - gap) / 2f

        val w1 = maxW * 0.75f
        drawLine(
            color,
            Offset(cx - w1 / 2, startY + sw / 2f),
            Offset(cx + w1 / 2, startY + sw / 2f),
            sw,
            StrokeCap.Round,
        )
        val w2 = maxW * 0.5f
        drawLine(
            color,
            Offset(cx - w2 / 2, startY + sw + gap + sw / 2f),
            Offset(cx + w2 / 2, startY + sw + gap + sw / 2f),
            sw,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun GraffitiAngleLeftIcon(tint: Color) {
    GraffitiAngleIcon(tint, -1f)
}

@Composable
private fun GraffitiAngleCenterIcon(tint: Color) {
    GraffitiAngleIcon(tint, 0f)
}

@Composable
private fun GraffitiAngleRightIcon(tint: Color) {
    GraffitiAngleIcon(tint, 1f)
}

@Composable
private fun GraffitiAngleIcon(tint: Color, direction: Float) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.5f.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = 9.dp.toPx()
        val tilt = direction * 5.dp.toPx()
        drawLine(
            color.copy(alpha = 0.42f),
            Offset(cx - half, cy - 6.dp.toPx() + tilt),
            Offset(cx + half, cy - 6.dp.toPx() - tilt),
            sw,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(cx - half * 0.7f, cy + 3.dp.toPx() + tilt * 0.7f),
            Offset(cx + half * 0.7f, cy + 3.dp.toPx() - tilt * 0.7f),
            sw,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun DepthOnIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.dp.toPx()
        val cx = size.width / 2f
        val inset = 3.dp.toPx()
        val maxW = size.width - inset * 2

        val lineY1 = size.height * 0.3f
        val lineY2 = size.height * 0.5f
        drawLine(
            color.copy(alpha = 0.4f),
            Offset(cx - maxW * 0.35f, lineY1),
            Offset(cx + maxW * 0.35f, lineY1),
            sw,
            StrokeCap.Round,
        )
        drawLine(
            color.copy(alpha = 0.4f),
            Offset(cx - maxW * 0.25f, lineY2),
            Offset(cx + maxW * 0.25f, lineY2),
            sw,
            StrokeCap.Round,
        )

        val peakX = size.width * 0.55f
        val peakY = size.height * 0.25f
        val baseY = size.height * 0.78f
        val halfW = 6.dp.toPx()
        val peakSw = 1.8f.dp.toPx()
        drawLine(color, Offset(peakX - halfW, baseY), Offset(peakX, peakY), peakSw, StrokeCap.Round)
        drawLine(color, Offset(peakX, peakY), Offset(peakX + halfW, baseY), peakSw, StrokeCap.Round)
        drawLine(
            color,
            Offset(peakX - halfW, baseY),
            Offset(peakX + halfW, baseY),
            peakSw,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun DepthOffIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.dp.toPx()
        val cx = size.width / 2f
        val inset = 3.dp.toPx()
        val maxW = size.width - inset * 2

        val lineY1 = size.height * 0.3f
        val lineY2 = size.height * 0.5f
        val lineY3 = size.height * 0.7f
        drawLine(
            color,
            Offset(cx - maxW * 0.35f, lineY1),
            Offset(cx + maxW * 0.35f, lineY1),
            sw,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(cx - maxW * 0.25f, lineY2),
            Offset(cx + maxW * 0.25f, lineY2),
            sw,
            StrokeCap.Round,
        )
        drawLine(
            color.copy(alpha = 0.5f),
            Offset(cx - maxW * 0.2f, lineY3),
            Offset(cx + maxW * 0.2f, lineY3),
            sw * 0.8f,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun DateAboveIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.5f.dp.toPx()
        val thinSw = 1.5f.dp.toPx()
        val cx = size.width / 2f
        val inset = 3.dp.toPx()
        val maxW = size.width - inset * 2

        val dateY = size.height * 0.28f
        drawLine(
            color.copy(alpha = 0.5f),
            Offset(cx - maxW * 0.25f, dateY),
            Offset(cx + maxW * 0.25f, dateY),
            thinSw,
            StrokeCap.Round,
        )

        val lineY1 = size.height * 0.52f
        val lineY2 = size.height * 0.72f
        drawLine(
            color,
            Offset(cx - maxW * 0.38f, lineY1),
            Offset(cx + maxW * 0.38f, lineY1),
            sw,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(cx - maxW * 0.28f, lineY2),
            Offset(cx + maxW * 0.28f, lineY2),
            sw,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun DateBelowIcon(tint: Color) {
    OptionIcon(tint = tint) { color ->
        val sw = 2.5f.dp.toPx()
        val thinSw = 1.5f.dp.toPx()
        val cx = size.width / 2f
        val inset = 3.dp.toPx()
        val maxW = size.width - inset * 2

        val lineY1 = size.height * 0.28f
        val lineY2 = size.height * 0.48f
        drawLine(
            color,
            Offset(cx - maxW * 0.38f, lineY1),
            Offset(cx + maxW * 0.38f, lineY1),
            sw,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(cx - maxW * 0.28f, lineY2),
            Offset(cx + maxW * 0.28f, lineY2),
            sw,
            StrokeCap.Round,
        )

        val dateY = size.height * 0.72f
        drawLine(
            color.copy(alpha = 0.5f),
            Offset(cx - maxW * 0.25f, dateY),
            Offset(cx + maxW * 0.25f, dateY),
            thinSw,
            StrokeCap.Round,
        )
    }
}

private fun readCurrentClockId(context: Context): String {
    return normalizeClockId(context, readRawClockId(context))
}

private fun readRawClockId(context: Context): String {
    return try {
        val json =
            Settings.Secure.getString(
                context.contentResolver,
                ClockSettingsRepository.SETTING_CLOCK_FACE,
            )
        val clockId = if (!json.isNullOrEmpty()) {
            JSONObject(json).optString("clockId", "DEFAULT")
        } else {
            "DEFAULT"
        }
        clockId
    } catch (_: Exception) {
        "DEFAULT"
    }
}

private fun normalizeClockId(context: Context, clockId: String): String {
    val oplusClassic = context.resources.getString(AxClockType.OPLUS_CLASSIC.clockId)
    val oplusBig = context.resources.getString(AxClockType.OPLUS_BIG.clockId)
    return when (clockId) {
        "OPLUS_CLASSIC_STACKED",
        "OPLUS_CLASSIC_START_VERTICAL",
        "OPLUS_CLASSIC_END_VERTICAL",
        "OPLUS_CLASSIC_DUAL_VERTICAL" -> oplusClassic
        "OPLUS_BIG_CENTER_VERTICAL",
        "OPLUS_BIG_VERTICAL",
        "OPLUS_BIG_WIDE",
        "OPLUS_BIG_START_HORIZONTAL",
        "OPLUS_BIG_END_HORIZONTAL" -> oplusBig
        else -> clockId
    }
}

private fun readAlignment(context: Context): String {
    return Settings.Secure.getString(
        context.contentResolver,
        ClockSettingsRepository.SETTING_ALIGNMENT,
    ) ?: ClockSettingsRepository.ALIGNMENT_CENTER
}

private fun readDatePosition(context: Context): String {
    return Settings.Secure.getString(
        context.contentResolver,
        ClockSettingsRepository.SETTING_DATE_POSITION,
    ) ?: ClockSettingsRepository.DATE_POSITION_ABOVE
}

private fun readDepthEnabled(context: Context): Boolean {
    return Settings.Secure.getInt(context.contentResolver, DEPTH_SETTINGS_KEY, 0) == 1
}

private fun readClockColor(context: Context): String {
    return Settings.Secure.getString(
        context.contentResolver,
        ClockSettingsRepository.SETTING_CLOCK_COLOR,
    ) ?: ClockSettingsRepository.COLOR_AUTO
}

private fun readOplusClassicFace(context: Context): String {
    val selected = when (
        Settings.Secure.getString(
            context.contentResolver,
            ClockSettingsRepository.SETTING_OPLUS_CLASSIC_FACE,
        )
    ) {
        ClockSettingsRepository.OPLUS_CLASSIC_FACE_STACKED ->
            ClockSettingsRepository.OPLUS_CLASSIC_FACE_STACKED
        else -> ClockSettingsRepository.OPLUS_CLASSIC_FACE_DEFAULT
    }
    if (selected != ClockSettingsRepository.OPLUS_CLASSIC_FACE_DEFAULT) return selected
    return when (readRawClockId(context)) {
        "OPLUS_CLASSIC_STACKED",
        "OPLUS_CLASSIC_START_VERTICAL",
        "OPLUS_CLASSIC_END_VERTICAL",
        "OPLUS_CLASSIC_DUAL_VERTICAL" ->
            ClockSettingsRepository.OPLUS_CLASSIC_FACE_STACKED
        else -> selected
    }
}

private fun readOplusBigFace(context: Context): String {
    val selected = when (
        Settings.Secure.getString(
            context.contentResolver,
            ClockSettingsRepository.SETTING_OPLUS_BIG_FACE,
        )
    ) {
        ClockSettingsRepository.OPLUS_BIG_FACE_STRETCH ->
            ClockSettingsRepository.OPLUS_BIG_FACE_WIDE
        ClockSettingsRepository.OPLUS_BIG_FACE_WIDE ->
            ClockSettingsRepository.OPLUS_BIG_FACE_WIDE
        else -> ClockSettingsRepository.OPLUS_BIG_FACE_DEFAULT
    }
    if (selected != ClockSettingsRepository.OPLUS_BIG_FACE_DEFAULT) return selected
    return when (readRawClockId(context)) {
        "OPLUS_BIG_VERTICAL",
        "OPLUS_BIG_START_HORIZONTAL",
        "OPLUS_BIG_END_HORIZONTAL" ->
            ClockSettingsRepository.OPLUS_BIG_FACE_WIDE
        "OPLUS_BIG_WIDE" ->
            ClockSettingsRepository.OPLUS_BIG_FACE_WIDE
        else -> selected
    }
}

private fun readOplusBigDualTone(context: Context): Boolean {
    return Settings.Secure.getInt(
        context.contentResolver,
        ClockSettingsRepository.SETTING_OPLUS_BIG_DUAL_TONE,
        0,
    ) == 1
}

private fun readOplusGraffitiFace(context: Context): String {
    return when (
        Settings.Secure.getString(
            context.contentResolver,
            ClockSettingsRepository.SETTING_OPLUS_GRAFFITI_FACE,
        )
    ) {
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_SANS ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_SANS
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_BRIGHT ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_BRIGHT
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_CUTE ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_CUTE
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_DIGIT04 ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_DIGIT04
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_GAME ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_GAME
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_KEEP ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_KEEP
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_WENDAO ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_WENDAO
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_SHENQI ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_SHENQI
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_GALADA ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_GALADA
        ClockSettingsRepository.OPLUS_GRAFFITI_FACE_MODAK ->
            ClockSettingsRepository.OPLUS_GRAFFITI_FACE_MODAK
        else -> ClockSettingsRepository.OPLUS_GRAFFITI_FACE_DEFAULT
    }
}

private fun readOplusGraffitiAngle(context: Context): String {
    return when (
        Settings.Secure.getString(
            context.contentResolver,
            ClockSettingsRepository.SETTING_OPLUS_GRAFFITI_ANGLE,
        )
    ) {
        ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_LEFT ->
            ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_LEFT
        ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_RIGHT ->
            ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_RIGHT
        else -> ClockSettingsRepository.OPLUS_GRAFFITI_ANGLE_CENTER
    }
}

private val AxClockType.isOplusClock: Boolean
    get() = name.startsWith("OPLUS_")
