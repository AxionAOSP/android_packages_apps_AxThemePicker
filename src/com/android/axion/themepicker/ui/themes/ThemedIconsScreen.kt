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

package com.android.axion.themepicker.ui.themes

import android.R.color.system_accent1_100 as SystemAccent1_100
import android.R.color.system_accent1_200 as SystemAccent1_200
import android.R.color.system_accent1_600 as SystemAccent1_600
import android.R.color.system_accent1_700 as SystemAccent1_700
import android.R.color.system_accent2_800 as SystemAccent2_800
import android.R.color.system_neutral1_50 as SystemNeutral1_50
import android.R.color.system_neutral1_900 as SystemNeutral1_900
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ThemeEngine
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.compose.color.ColorPickerDialog
import com.android.axion.compose.color.ColorPickerPreset
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.theme.LocalAdaptiveLayoutInfo
import com.android.axion.themepicker.ui.theme.bounceable
import com.android.axion.themepicker.utils.math.scaleRatio
import com.android.axion.themepicker.viewmodel.MainScreenViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val SETTING_THEMED_ICONS_ENABLED = "themed_icons"
private const val SETTING_THEMED_ICON_PACK = "themed_icon_pack"
private const val SETTING_THEMED_ICON_SCALE = "themed_icon_scale"
private const val SETTING_THEMED_ICON_BACKGROUND = "themed_icon_background_color"
private const val SETTING_THEMED_ICON_FOREGROUND = "themed_icon_foreground_color"
private const val SETTINGS_THEME_ENGINE_DATA = "theme_engine_data"
private const val CATEGORY_ICON_PACK = "icon_pack"
private const val DEFAULT_THEMED_ICON_SCALE = 72
private const val MIN_THEMED_ICON_SCALE = 48
private const val MAX_THEMED_ICON_SCALE = 100

private const val ACTION_THEMED_ICON = "app.lawnchair.icons.THEMED_ICON"

private data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val isThemedPack: Boolean = false,
)

private data class ThemedIconColorDefault(
    val label: String,
    val background: Int,
    val foreground: Int,
)

private enum class ColorTarget {
    Background,
    Foreground,
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemedIconsScreen(mainScreenViewModel: MainScreenViewModel) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scale = context.scaleRatio
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val resolver = context.contentResolver

    var enabled by remember {
        mutableStateOf(Settings.Secure.getInt(resolver, SETTING_THEMED_ICONS_ENABLED, 0) == 1)
    }
    val darkTheme = isSystemInDarkTheme()
    val colorDefaults = remember(context, darkTheme) { themedIconColorDefaults(context, darkTheme) }
    var iconScale by remember { mutableStateOf(readThemedIconScale(context)) }
    var backgroundColor by remember(colorDefaults) {
        mutableStateOf(
            readSecureInt(
                context,
                SETTING_THEMED_ICON_BACKGROUND,
                colorDefaults.first().background,
            )
        )
    }
    var foregroundColor by remember(colorDefaults) {
        mutableStateOf(
            readSecureInt(
                context,
                SETTING_THEMED_ICON_FOREGROUND,
                colorDefaults.first().foreground,
            )
        )
    }

    fun updateIconScale(value: Int) {
        val coerced = value.coerceIn(MIN_THEMED_ICON_SCALE, MAX_THEMED_ICON_SCALE)
        iconScale = coerced
        Settings.Secure.putInt(resolver, SETTING_THEMED_ICON_SCALE, coerced)
    }

    fun updateBackgroundColor(value: Int) {
        backgroundColor = value
        Settings.Secure.putInt(resolver, SETTING_THEMED_ICON_BACKGROUND, value)
    }

    fun updateForegroundColor(value: Int) {
        foregroundColor = value
        Settings.Secure.putInt(resolver, SETTING_THEMED_ICON_FOREGROUND, value)
    }

    var iconPacks by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    var activeIconPack by remember { mutableStateOf<String?>(null) }
    var themedIconPacks by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    var activeThemedIconPack by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val engine = ThemeEngine.getInstance(context)
            val pm = context.packageManager
            val packs =
                mutableListOf(
                    IconPackInfo("", context.getString(R.string.icon_pack_system_default))
                )

            val installed = engine?.getInstalledIconPacks()
            installed?.forEach { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    packs.add(IconPackInfo(pkg, label, icon))
                } catch (_: PackageManager.NameNotFoundException) {}
            }

            iconPacks = packs
            activeIconPack = engine?.getIconPackPackage()

            try {
                val themedPacks = mutableListOf(
                    IconPackInfo("", context.getString(R.string.themed_icon_pack_none), isThemedPack = true)
                )
                val seenPackages = mutableSetOf<String>()

                try {
                    val themedIntent = Intent(ACTION_THEMED_ICON)
                    val themedResults = pm.queryIntentActivities(themedIntent, 0)
                    for (ri in themedResults) {
                        val pkg = ri.activityInfo.packageName
                        if (seenPackages.contains(pkg)) continue
                        seenPackages.add(pkg)
                        try {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val icon = pm.getApplicationIcon(appInfo)
                            themedPacks.add(IconPackInfo(pkg, label, icon, isThemedPack = true))
                        } catch (_: PackageManager.NameNotFoundException) {}
                    }
                } catch (_: Exception) {}

                installed?.forEach { pkg ->
                    if (seenPackages.contains(pkg)) return@forEach
                    try {
                        val res = pm.getResourcesForApplication(pkg)
                        val mapId = res.getIdentifier("grayscale_icon_map", "xml", pkg)
                        if (mapId != 0) {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val icon = pm.getApplicationIcon(appInfo)
                            themedPacks.add(IconPackInfo(pkg, label, icon, isThemedPack = true))
                            seenPackages.add(pkg)
                        }
                    } catch (_: Exception) {}
                }

                themedIconPacks = themedPacks
                activeThemedIconPack = Settings.Secure.getString(
                    resolver, SETTING_THEMED_ICON_PACK)
            } catch (_: Exception) {}
        }
    }

    BackHandler { mainScreenViewModel.goBack() }

    AxionScaffold(
        title = stringResource(R.string.themed_icons_title),
        onBackClick = { mainScreenViewModel.goBack() },
        modifier = Modifier.background(colors.background),
    ) { paddingValues ->
        if (layoutInfo.isDualPane) {
            Row(
                modifier =
                    Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    modifier =
                        Modifier.weight(0.4f)
                            .fillMaxHeight()
                            .padding(vertical = 24.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(colors.surfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    ThemedIconPreviewLarge(
                        scale = iconScale,
                        backgroundColor = backgroundColor,
                        foregroundColor = foregroundColor,
                        enabled = enabled,
                    )
                }

                Column(
                    modifier =
                        Modifier.weight(0.6f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ThemedIconSettingsCard(
                        enabled = enabled,
                        iconScale = iconScale,
                        backgroundColor = backgroundColor,
                        foregroundColor = foregroundColor,
                        colorDefaults = colorDefaults,
                        onEnabledChange = { newEnabled ->
                            enabled = newEnabled
                            Settings.Secure.putInt(
                                resolver,
                                SETTING_THEMED_ICONS_ENABLED,
                                if (newEnabled) 1 else 0,
                            )
                        },
                        onIconScaleChange = ::updateIconScale,
                        onBackgroundColorChange = ::updateBackgroundColor,
                        onForegroundColorChange = ::updateForegroundColor,
                    )

                    IconPackCard(
                        iconPacks = iconPacks,
                        activeIconPack = activeIconPack,
                        onSelectPack = { pkg ->
                            applyIconPack(context, pkg)
                            activeIconPack = pkg.ifEmpty { null }
                        },
                    )

                    if (themedIconPacks.size > 1) {
                        ThemedIconPackCard(
                            themedPacks = themedIconPacks,
                            activeThemedPack = activeThemedIconPack,
                            onSelectPack = { pkg ->
                                Settings.Secure.putString(
                                    resolver, SETTING_THEMED_ICON_PACK,
                                    pkg.ifEmpty { null })
                                activeThemedIconPack = pkg.ifEmpty { null }
                            },
                        )
                    }
                }
            }
        } else {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp * scale),
                    contentAlignment = Alignment.Center,
                ) {
                    ThemedIconPreviewLarge(
                        scale = iconScale,
                        backgroundColor = backgroundColor,
                        foregroundColor = foregroundColor,
                        enabled = enabled,
                    )
                }

                ThemedIconSettingsCard(
                    enabled = enabled,
                    iconScale = iconScale,
                    backgroundColor = backgroundColor,
                    foregroundColor = foregroundColor,
                    colorDefaults = colorDefaults,
                    onEnabledChange = { newEnabled ->
                        enabled = newEnabled
                        Settings.Secure.putInt(
                            resolver,
                            SETTING_THEMED_ICONS_ENABLED,
                            if (newEnabled) 1 else 0,
                        )
                    },
                    onIconScaleChange = ::updateIconScale,
                    onBackgroundColorChange = ::updateBackgroundColor,
                    onForegroundColorChange = ::updateForegroundColor,
                    modifier = Modifier.padding(horizontal = 16.dp * scale),
                )

                Spacer(modifier = Modifier.height(16.dp * scale))

                IconPackCard(
                    iconPacks = iconPacks,
                    activeIconPack = activeIconPack,
                    onSelectPack = { pkg ->
                        applyIconPack(context, pkg)
                        activeIconPack = pkg.ifEmpty { null }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp * scale),
                )

                if (themedIconPacks.size > 1) {
                    Spacer(modifier = Modifier.height(16.dp * scale))

                    ThemedIconPackCard(
                        themedPacks = themedIconPacks,
                        activeThemedPack = activeThemedIconPack,
                        onSelectPack = { pkg ->
                            Settings.Secure.putString(
                                resolver, SETTING_THEMED_ICON_PACK,
                                pkg.ifEmpty { null })
                            activeThemedIconPack = pkg.ifEmpty { null }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp * scale),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ThemedIconPreviewLarge(
    scale: Int,
    backgroundColor: Int,
    foregroundColor: Int,
    enabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.size(200.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (enabled) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PreviewIcon(scale, backgroundColor, foregroundColor, IconType.PHONE)
                        PreviewIcon(scale, backgroundColor, foregroundColor, IconType.MESSAGES)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PreviewIcon(scale, backgroundColor, foregroundColor, IconType.CAMERA)
                        PreviewIcon(scale, backgroundColor, foregroundColor, IconType.SETTINGS)
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.off),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemedIconSettingsCard(
    enabled: Boolean,
    iconScale: Int,
    backgroundColor: Int,
    foregroundColor: Int,
    colorDefaults: List<ThemedIconColorDefault>,
    onEnabledChange: (Boolean) -> Unit,
    onIconScaleChange: (Int) -> Unit,
    onBackgroundColorChange: (Int) -> Unit,
    onForegroundColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scale = LocalContext.current.scaleRatio
    val scaleRange = remember { MIN_THEMED_ICON_SCALE.toFloat()..MAX_THEMED_ICON_SCALE.toFloat() }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp * scale)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.themed_icons_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.themed_icons_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.primary,
                        checkedTrackColor = colors.primaryContainer,
                    ),
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.slowEffectsSpec()) +
                    expandVertically(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.slowEffectsSpec()) +
                    shrinkVertically(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp * scale))
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(20.dp * scale))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.themed_icon_size_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.themed_icon_size_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(R.string.themed_icon_size_percent, iconScale),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                    }

                    Slider(
                        value = iconScale.toFloat(),
                        onValueChange = { onIconScaleChange(it.roundToInt()) },
                        valueRange = scaleRange,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.surfaceContainerHighest,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp * scale))

                    ThemedIconColorDefaultsRow(
                        colorDefaults = colorDefaults,
                        backgroundColor = backgroundColor,
                        foregroundColor = foregroundColor,
                        onSelect = { preset ->
                            onBackgroundColorChange(preset.background)
                            onForegroundColorChange(preset.foreground)
                        },
                    )

                    Spacer(modifier = Modifier.height(12.dp * scale))

                    ThemedIconColorRow(
                        title = stringResource(R.string.themed_icon_background_title),
                        color = backgroundColor,
                        onClick = { colorTarget = ColorTarget.Background },
                    )

                    Spacer(modifier = Modifier.height(8.dp * scale))

                    ThemedIconColorRow(
                        title = stringResource(R.string.themed_icon_foreground_title),
                        color = foregroundColor,
                        onClick = { colorTarget = ColorTarget.Foreground },
                    )
                }
            }
        }
    }

    colorTarget?.let { target ->
        val isBackground = target == ColorTarget.Background
        val title = stringResource(
            if (isBackground) R.string.themed_icon_background_title
            else R.string.themed_icon_foreground_title,
        )
        ColorPickerDialog(
            initialColor = Color(if (isBackground) backgroundColor else foregroundColor),
            title = title,
            presets = colorDefaults.map { preset ->
                ColorPickerPreset(
                    label = preset.label,
                    color = Color(if (isBackground) preset.background else preset.foreground),
                )
            },
            defaultsLabel = stringResource(R.string.themed_icon_color_defaults_title),
            onDismiss = { colorTarget = null },
            onColorSelected = { color ->
                if (isBackground) {
                    onBackgroundColorChange(color.toArgb())
                } else {
                    onForegroundColorChange(color.toArgb())
                }
                colorTarget = null
            },
        )
    }
}

@Composable
private fun ThemedIconColorDefaultsRow(
    colorDefaults: List<ThemedIconColorDefault>,
    backgroundColor: Int,
    foregroundColor: Int,
    onSelect: (ThemedIconColorDefault) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column {
        Text(
            text = stringResource(R.string.themed_icon_color_defaults_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.themed_icon_color_defaults_summary),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colorDefaults.forEach { preset ->
                ThemedIconColorDefaultChip(
                    preset = preset,
                    selected = backgroundColor == preset.background &&
                        foregroundColor == preset.foreground,
                    onClick = { onSelect(preset) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemedIconColorDefaultChip(
    preset: ThemedIconColorDefault,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.height(56.dp).bounceable(onClick = onClick, scale = 0.98f),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        ),
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemedIconPairSwatch(preset.background, preset.foreground)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = preset.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            )
        }
    }
}

@Composable
private fun ThemedIconColorRow(title: String, color: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth().bounceable(onClick = onClick, scale = 0.98f),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemedIconSingleSwatch(color)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )
                Text(
                    text = color.toHexColorString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ThemedIconPairSwatch(backgroundColor: Int, foregroundColor: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(Color(backgroundColor))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(Color(foregroundColor)),
        )
    }
}

@Composable
private fun ThemedIconSingleSwatch(color: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(Color(color))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
    )
}

@Composable
private fun IconPackCard(
    iconPacks: List<IconPackInfo>,
    activeIconPack: String?,
    onSelectPack: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scale = LocalContext.current.scaleRatio

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp * scale)) {
            Text(
                text = stringResource(R.string.icon_pack_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.icon_pack_description),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp * scale))

            iconPacks.forEach { pack ->
                val isActive =
                    if (pack.packageName.isEmpty()) {
                        activeIconPack.isNullOrEmpty()
                    } else {
                        pack.packageName == activeIconPack
                    }

                IconPackItem(
                    pack = pack,
                    isActive = isActive,
                    onClick = { onSelectPack(pack.packageName) },
                )
            }
        }
    }
}

@Composable
private fun IconPackItem(pack: IconPackInfo, isActive: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 4.dp)
                .bounceable(onClick = onClick, scale = 0.98f),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isActive) colors.primaryContainer.copy(alpha = 0.3f)
                    else colors.surfaceContainerHigh
            ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pack.icon != null) {
                Image(
                    bitmap = pack.icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = pack.label,
                    modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.medium),
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pack.label.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (pack.packageName.isNotEmpty()) {
                    Text(
                        text = pack.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.icon_pack_active),
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemedIconPackCard(
    themedPacks: List<IconPackInfo>,
    activeThemedPack: String?,
    onSelectPack: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scale = LocalContext.current.scaleRatio

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp * scale)) {
            Text(
                text = stringResource(R.string.themed_icon_pack_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.themed_icon_pack_description),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp * scale))

            themedPacks.forEach { pack ->
                val isActive =
                    if (pack.packageName.isEmpty()) {
                        activeThemedPack.isNullOrEmpty()
                    } else {
                        pack.packageName == activeThemedPack
                    }

                IconPackItem(
                    pack = pack,
                    isActive = isActive,
                    onClick = { onSelectPack(pack.packageName) },
                )
            }
        }
    }
}


private fun themedIconColorDefaults(
    context: Context,
    isDark: Boolean,
): List<ThemedIconColorDefault> = listOf(
    ThemedIconColorDefault(
        label = context.getString(R.string.themed_icon_color_preset_axicons),
        background = context.getColor(if (isDark) SystemNeutral1_900 else SystemNeutral1_50),
        foreground = context.getColor(if (isDark) SystemAccent1_100 else SystemAccent1_600),
    ),
    ThemedIconColorDefault(
        label = context.getString(R.string.themed_icon_color_preset_aosp),
        background = context.getColor(if (isDark) SystemAccent2_800 else SystemAccent1_100),
        foreground = context.getColor(if (isDark) SystemAccent1_200 else SystemAccent1_700),
    ),
)

private fun readThemedIconScale(context: Context): Int = readSecureInt(
    context = context,
    key = SETTING_THEMED_ICON_SCALE,
    defaultValue = DEFAULT_THEMED_ICON_SCALE,
).coerceIn(MIN_THEMED_ICON_SCALE, MAX_THEMED_ICON_SCALE)

private fun readSecureInt(context: Context, key: String, defaultValue: Int): Int =
    Settings.Secure.getInt(context.contentResolver, key, defaultValue)

private fun Int.toHexColorString(): String = String.format("#%06X", 0xFFFFFF and this)

private fun applyIconPack(context: Context, packageName: String) {
    try {
        val resolver = context.contentResolver
        val json = Settings.Secure.getString(resolver, SETTINGS_THEME_ENGINE_DATA)
        val config = if (json.isNullOrBlank()) JSONObject() else JSONObject(json)

        val themes = config.optJSONObject("themes") ?: JSONObject()
        val iconPackConfig = JSONObject()
        if (packageName.isEmpty()) {
            iconPackConfig.put("enabled", false)
            iconPackConfig.put("packageName", JSONObject.NULL)
        } else {
            iconPackConfig.put("enabled", true)
            iconPackConfig.put("packageName", packageName)
        }
        themes.put(CATEGORY_ICON_PACK, iconPackConfig)
        config.put("themes", themes)

        if (!config.has("version")) config.put("version", 1)

        Settings.Secure.putString(resolver, SETTINGS_THEME_ENGINE_DATA, config.toString())
    } catch (_: Exception) {}
}

private enum class IconType {
    PHONE,
    MESSAGES,
    CAMERA,
    SETTINGS,
}

@Composable
private fun PreviewIcon(
    scale: Int,
    backgroundColor: Int,
    foregroundColor: Int,
    type: IconType,
) {
    val bgColor = Color(backgroundColor)
    val fgColor = Color(foregroundColor)
    val iconSize = (36f * scale / 100f).dp

    Box(
        modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.extraLarge).background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        val surfaceBrightColor = MaterialTheme.colorScheme.surfaceBright
        Canvas(modifier = Modifier.size(iconSize)) {
            val canvasSize = size.minDimension
            val strokeWidth = canvasSize * 0.12f

            when (type) {
                IconType.PHONE -> {
                    val path =
                        Path().apply {
                            val w = canvasSize
                            val h = canvasSize
                            moveTo(w * 0.35f, h * 0.15f)
                            cubicTo(w * 0.25f, h * 0.15f, w * 0.2f, h * 0.2f, w * 0.2f, h * 0.3f)
                            lineTo(w * 0.2f, h * 0.7f)
                            cubicTo(w * 0.2f, h * 0.8f, w * 0.25f, h * 0.85f, w * 0.35f, h * 0.85f)
                            lineTo(w * 0.65f, h * 0.85f)
                            cubicTo(w * 0.75f, h * 0.85f, w * 0.8f, h * 0.8f, w * 0.8f, h * 0.7f)
                            lineTo(w * 0.8f, h * 0.3f)
                            cubicTo(w * 0.8f, h * 0.2f, w * 0.75f, h * 0.15f, w * 0.65f, h * 0.15f)
                            close()
                        }
                    drawPath(path, fgColor, style = Stroke(strokeWidth))
                    drawCircle(
                        fgColor,
                        radius = canvasSize * 0.05f,
                        center = Offset(canvasSize * 0.5f, canvasSize * 0.75f),
                    )
                }
                IconType.MESSAGES -> {
                    val path =
                        Path().apply {
                            val w = canvasSize
                            val h = canvasSize
                            moveTo(w * 0.15f, h * 0.3f)
                            cubicTo(w * 0.15f, h * 0.2f, w * 0.2f, h * 0.15f, w * 0.3f, h * 0.15f)
                            lineTo(w * 0.7f, h * 0.15f)
                            cubicTo(w * 0.8f, h * 0.15f, w * 0.85f, h * 0.2f, w * 0.85f, h * 0.3f)
                            lineTo(w * 0.85f, h * 0.6f)
                            cubicTo(w * 0.85f, h * 0.7f, w * 0.8f, h * 0.75f, w * 0.7f, h * 0.75f)
                            lineTo(w * 0.55f, h * 0.75f)
                            lineTo(w * 0.45f, h * 0.85f)
                            lineTo(w * 0.45f, h * 0.75f)
                            lineTo(w * 0.3f, h * 0.75f)
                            cubicTo(w * 0.2f, h * 0.75f, w * 0.15f, h * 0.7f, w * 0.15f, h * 0.6f)
                            close()
                        }
                    drawPath(path, fgColor)
                }
                IconType.CAMERA -> {
                    drawRoundRect(
                        fgColor,
                        topLeft = Offset(canvasSize * 0.15f, canvasSize * 0.3f),
                        size = Size(canvasSize * 0.7f, canvasSize * 0.5f),
                        cornerRadius = CornerRadius(canvasSize * 0.08f),
                        style = Stroke(strokeWidth),
                    )
                    drawCircle(
                        fgColor,
                        radius = canvasSize * 0.15f,
                        center = Offset(canvasSize * 0.5f, canvasSize * 0.55f),
                        style = Stroke(strokeWidth),
                    )
                    drawRect(
                        fgColor,
                        topLeft = Offset(canvasSize * 0.35f, canvasSize * 0.2f),
                        size = Size(canvasSize * 0.3f, canvasSize * 0.1f),
                    )
                }
                IconType.SETTINGS -> {
                    val centerX = canvasSize * 0.5f
                    val centerY = canvasSize * 0.5f
                    val outerRadius = canvasSize * 0.35f
                    val innerRadius = canvasSize * 0.15f

                    val path = Path()
                    for (i in 0 until 6) {
                        val angle = (i * 60f - 90f) * (Math.PI / 180f).toFloat()
                        val x = centerX + outerRadius * cos(angle)
                        val y = centerY + outerRadius * sin(angle)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                        val nextAngle = ((i + 1) * 60f - 90f) * (Math.PI / 180f).toFloat()
                        val midAngle = (angle + nextAngle) / 2f
                        val midX = centerX + innerRadius * cos(midAngle)
                        val midY = centerY + innerRadius * sin(midAngle)
                        path.lineTo(midX, midY)
                    }
                    path.close()

                    drawPath(path, fgColor)
                    drawCircle(
                        surfaceBrightColor,
                        radius = canvasSize * 0.12f,
                        center = Offset(centerX, centerY),
                    )
                }
            }
        }
    }
}
