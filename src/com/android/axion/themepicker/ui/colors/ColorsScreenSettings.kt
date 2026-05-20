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

package com.android.axion.themepicker.ui.colors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.axion.themepicker.R
import com.android.axion.compose.color.ColorPickerDialog
import com.android.axion.themepicker.ui.dialogs.StylePickerDialog
import com.android.axion.themepicker.ui.dialogs.WallpaperColorPickerDialog
import com.android.axion.themepicker.ui.theme.bounceable
import com.android.axion.themepicker.utils.colors.toArgb
import com.android.axion.themepicker.utils.settings.applyColorSettings
import com.android.axion.themepicker.utils.settings.loadCurrentSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BasicColorsSettings() {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(loadCurrentSettings(context, accent)) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var showStylePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ColorSourceCard(
            useWallpaper = settings.useWallpaperColors,
            seedColor = settings.seedColor,
            onToggleWallpaper = { enabled ->
                settings = settings.copy(useWallpaperColors = enabled)
                scope.launch {
                    delay(350)
                    applyColorSettings(context, settings)
                }
            },
            onPickColor = {
                settings = settings.copy(useWallpaperColors = false)
                showColorPicker = true
            },
            onPickFromWallpaper = {
                settings = settings.copy(useWallpaperColors = false)
                showWallpaperPicker = true
            },
        )

        ThemeStyleCard(
            currentStyle = settings.style.displayName,
            enabled = !settings.fidelity,
            onClick = { showStylePicker = true },
        )

        AdvancedColorsCard(
            fidelity = settings.fidelity,
            contrastLevel = settings.contrastLevel,
            chromaBoost = settings.chromaBoost,
            onFidelityChange = { enabled ->
                settings = settings.copy(fidelity = enabled)
                applyColorSettings(context, settings)
            },
            onContrastChange = { value ->
                settings = settings.copy(contrastLevel = value)
                applyColorSettings(context, settings)
            },
            onChromaChange = { value ->
                settings = settings.copy(chromaBoost = value)
                applyColorSettings(context, settings)
            },
        )

        InfoFooter()
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = settings.seedColor,
            title = stringResource(R.string.choose_seed_color),
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                settings = settings.copy(seedColor = color)
                applyColorSettings(context, settings)
                showColorPicker = false
            },
        )
    }

    if (showWallpaperPicker) {
        WallpaperColorPickerDialog(
            onDismiss = { showWallpaperPicker = false },
            onColorSelected = { color ->
                settings = settings.copy(seedColor = color)
                applyColorSettings(context, settings)
                showWallpaperPicker = false
            },
        )
    }

    if (showStylePicker) {
        StylePickerDialog(
            currentStyle = settings.style,
            onDismiss = { showStylePicker = false },
            onStyleSelected = { style ->
                settings = settings.copy(style = style)
                applyColorSettings(context, settings)
                showStylePicker = false
            },
        )
    }
}

@Composable
private fun ColorSourceCard(
    useWallpaper: Boolean,
    seedColor: Color,
    onToggleWallpaper: (Boolean) -> Unit,
    onPickColor: () -> Unit,
    onPickFromWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceBright),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.theme_colors),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text =
                            if (useWallpaper) stringResource(R.string.following_wallpaper)
                            else stringResource(R.string.using_custom_color),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }

                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (useWallpaper) colors.primaryContainer else seedColor)
                            .border(
                                2.dp,
                                if (useWallpaper) colors.primary
                                else colors.outline.copy(alpha = 0.5f),
                                MaterialTheme.shapes.medium,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (useWallpaper) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModeChip(
                    text = stringResource(R.string.automatic),
                    icon = Icons.Filled.AutoAwesome,
                    isSelected = useWallpaper,
                    onClick = { onToggleWallpaper(true) },
                    modifier = Modifier.weight(1f),
                )
                ModeChip(
                    text = stringResource(R.string.custom),
                    icon = Icons.Filled.Palette,
                    isSelected = !useWallpaper,
                    onClick = { onToggleWallpaper(false) },
                    modifier = Modifier.weight(1f),
                )
            }

            AnimatedVisibility(
                visible = !useWallpaper,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.choose_custom_color_method),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ActionButton(
                            text = stringResource(R.string.from_wallpaper),
                            icon = Icons.Filled.Colorize,
                            onClick = onPickFromWallpaper,
                            modifier = Modifier.weight(1f),
                        )
                        ActionButton(
                            text = stringResource(R.string.color_picker),
                            icon = Icons.Filled.ColorLens,
                            onClick = onPickColor,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = colors.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(40.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(seedColor)
                                        .border(
                                            1.dp,
                                            colors.outlineVariant,
                                            MaterialTheme.shapes.small,
                                        )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.selected_color),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                                Text(
                                    text =
                                        "#${String.format("%06X", 0xFFFFFF and seedColor.toArgb())}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = modifier.height(48.dp),
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun ThemeStyleCard(
    currentStyle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val cardModifier =
        if (enabled) modifier.fillMaxWidth().bounceable(onClick = onClick, scale = 0.97f)
        else modifier.fillMaxWidth().alpha(0.5f)
    val subtitleText =
        if (enabled) currentStyle else stringResource(R.string.theme_style_disabled_by_fidelity)
    val subtitleColor = if (enabled) colors.primary else colors.onSurfaceVariant

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceBright),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = colors.secondaryContainer,
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Style,
                            contentDescription = null,
                            tint = colors.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.theme_style_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdvancedColorsCard(
    fidelity: Boolean,
    contrastLevel: Float,
    chromaBoost: Float,
    onFidelityChange: (Boolean) -> Unit,
    onContrastChange: (Float) -> Unit,
    onChromaChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceBright),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = colors.tertiaryContainer,
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = null,
                            tint = colors.onTertiaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.fine_tuning),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

            ExpressiveToggleRow(
                title = stringResource(R.string.fidelity_title),
                subtitle = stringResource(R.string.fidelity_desc),
                checked = fidelity,
                onCheckedChange = onFidelityChange,
            )

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

            ExpressiveSlider(
                title = stringResource(R.string.contrast_level_title),
                value = contrastLevel,
                onValueChange = onContrastChange,
                valueRange = -1f..1f,
                valueLabel =
                    when {
                        contrastLevel < -0.3f -> stringResource(R.string.contrast_low)
                        contrastLevel > 0.3f -> stringResource(R.string.contrast_high)
                        else -> stringResource(R.string.contrast_normal)
                    },
            )

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

            ExpressiveSlider(
                title = stringResource(R.string.chroma_boost_title),
                value = chromaBoost,
                onValueChange = onChromaChange,
                valueRange = 0f..100f,
                valueLabel = "${chromaBoost.toInt()}%",
            )
        }
    }
}

@Composable
private fun ExpressiveToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ExpressiveSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var sliderValue by remember(value) { mutableStateOf(value) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Surface(shape = MaterialTheme.shapes.extraSmall, color = colors.primaryContainer) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue) },
            valueRange = valueRange,
            colors =
                SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.primaryContainer,
                ),
        )
    }
}

@Composable
private fun InfoFooter(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text = stringResource(id = R.string.about_color_customization_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(id = R.string.about_color_customization_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
