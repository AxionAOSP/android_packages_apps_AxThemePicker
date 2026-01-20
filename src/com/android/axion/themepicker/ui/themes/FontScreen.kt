/*
 * Copyright (C) 2025 AxionOS
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

import android.graphics.Typeface as GraphicsTypeface
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.axion.themepicker.ui.components.FooterCard
import com.android.axion.themepicker.ui.expressive.ExpressiveDialog
import com.android.axion.themepicker.ui.expressive.ExpressiveHeader
import androidx.compose.material3.MaterialTheme
import com.android.axion.themepicker.utils.math.sdp
import com.android.axion.themepicker.viewmodel.MainScreenViewModel
import com.android.customization.model.ResourceConstants
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.axion.themepicker.R
import com.android.axion.themepicker.providers.ExternalFontInstaller
import com.android.axion.themepicker.providers.CommonOverlayProvider
import com.android.axion.themepicker.data.model.FontOverlayOption
import kotlinx.coroutines.*

@Composable
fun FontScreen(
    mainScreenViewModel: MainScreenViewModel
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fontOptions by remember { mutableStateOf<List<FontOverlayOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableStateOf(0) }
    var isApplying by remember { mutableStateOf(false) }
    var hasCustomFont by remember { mutableStateOf(false) }
    var customFontName by rememberSaveable { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var showFontPreviewDialog by remember { mutableStateOf(false) }
    var previewFontUri by remember { mutableStateOf<Uri?>(null) }
    var previewFontTypeface by remember { mutableStateOf<GraphicsTypeface?>(null) }

    val activeFontOption = remember(fontOptions) {
        fontOptions.firstOrNull { it.isActive }
    }
    val uiFontFamily = remember(activeFontOption) {
        activeFontOption?.bodyFont?.let { FontFamily(it) } ?: FontFamily.Default
    }

    val overlayProvider = remember {
        CommonOverlayProvider(
            context,
            OverlayManagerCompat(context),
            ResourceConstants.OVERLAY_CATEGORY_FONT
        )
    }
    val externalFontInstaller = remember { ExternalFontInstaller(context) }

    val fontPickerLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let { fontUri ->
            scope.launch {
                val typeface = externalFontInstaller.loadTypefaceFromUri(fontUri)
                previewFontTypeface = typeface
                if (typeface != null) {
                    previewFontUri = fontUri
                    showFontPreviewDialog = true
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_invalid_font_file),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val loadedOptions = overlayProvider.loadFontOptions()
        fontOptions = listOf(loadedOptions.first()) + loadedOptions.drop(1).sortedBy { it.label }
        val activeIndex = fontOptions.indexOfFirst { it.isActive }
        if (activeIndex >= 0) selectedIndex = activeIndex
        val customFontInstalled = Settings.Secure.getString(context.contentResolver, "custom_font_name") ?: ""
        customFontName = customFontInstalled
        hasCustomFont = customFontInstalled.isNotEmpty()
        isLoading = false
    }

    val listState = rememberLazyListState()

    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
        Box(Modifier.fillMaxSize().background(colors.background)) {
            Column(Modifier.fillMaxSize()) {
                ExpressiveHeader(
                    title = stringResource(R.string.font_title),
                    subtitle = fontOptions.getOrNull(selectedIndex)?.label ?: "",
                    onBackClick = { mainScreenViewModel.goBack() },
                    onActionClick = { showResetDialog = true }
                )

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(56.sdp), strokeWidth = 5.sdp)
                    }
                } else {
                    FontContent(
                        fontOptions = fontOptions,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it },
                        onApply = {
                            scope.launch {
                                isApplying = true
                                if (selectedIndex > 0) {
                                    externalFontInstaller.resetFontUpdates()
                                    Settings.Secure.putString(context.contentResolver, "custom_font_name", "")
                                    hasCustomFont = false
                                    customFontName = ""
                                }
                                val success = overlayProvider.applyOverlay(fontOptions[selectedIndex])
                                if (success) {
                                    fontOptions = fontOptions.mapIndexed { i, o -> o.copy(isActive = i == selectedIndex) }
                                }
                                isApplying = false
                            }
                        },
                        isApplying = isApplying,
                        hasCustomFont = hasCustomFont,
                        customFontName = customFontName,
                        onPickFont = {
                            fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf"))
                        },
                        onResetCustomFont = {
                            scope.launch {
                                externalFontInstaller.resetFontUpdates()
                                Settings.Secure.putString(context.contentResolver, "custom_font_name", "")
                                hasCustomFont = false
                                customFontName = ""
                                val updatedOptions = overlayProvider.loadFontOptions()
                                fontOptions = listOf(updatedOptions.first()) + updatedOptions.drop(1).sortedBy { it.label }
                                overlayProvider.applyOverlay(fontOptions[0])
                                fontOptions = fontOptions.mapIndexed { i, o -> o.copy(isActive = i == 0) }
                                selectedIndex = 0
                            }
                        },
                        listState = listState,
                        uiFontFamily = uiFontFamily
                    )
                }
            }
        }
    }

    if (showFontPreviewDialog && previewFontTypeface != null) {
        FontPreviewDialog(
            typeface = previewFontTypeface!!,
            onDismiss = {
                showFontPreviewDialog = false
                previewFontUri = null
                previewFontTypeface = null
            },
            onConfirm = {
                showFontPreviewDialog = false
                scope.launch {
                    previewFontUri?.let { uri ->
                        val postScriptName = externalFontInstaller.installFontFromUri(uri)
                        if (postScriptName != null) {
                            Settings.Secure.putString(context.contentResolver, "custom_font_name", postScriptName)
                            customFontName = postScriptName
                            hasCustomFont = true
                            val updatedOptions = overlayProvider.loadFontOptions()
                            fontOptions = listOf(updatedOptions.first()) + updatedOptions.drop(1).sortedBy { it.label }
                            selectedIndex = 0
                        }
                    }
                    previewFontUri = null
                    previewFontTypeface = null
                }
            }
        )
    }

    if (showResetDialog) {
        ExpressiveDialog(
            showDialog = showResetDialog,
            onDismiss = { showResetDialog = false },
            title = stringResource(R.string.reset_default_title),
            message = stringResource(R.string.reset_default_message),
            confirmText = stringResource(R.string.reset),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showResetDialog = false
                scope.launch {
                    if (hasCustomFont) {
                        externalFontInstaller.resetFontUpdates()
                        Settings.Secure.putString(context.contentResolver, "custom_font_name", "")
                        hasCustomFont = false
                        customFontName = ""
                    }
                    overlayProvider.applyOverlay(fontOptions[0])
                    val updatedOptions = overlayProvider.loadFontOptions()
                    fontOptions = listOf(updatedOptions.first()) + updatedOptions.drop(1).sortedBy { it.label }
                    fontOptions = fontOptions.mapIndexed { i, o -> o.copy(isActive = i == 0) }
                    selectedIndex = 0
                }
            }
        )
    }
}

@Composable
private fun FontPreviewDialog(
    typeface: GraphicsTypeface,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.sdp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.sdp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.sdp)
            ) {
                Text(
                    text = stringResource(R.string.font_preview_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                
                Spacer(Modifier.height(24.sdp))
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.sdp),
                    color = colors.surfaceBright,
                    shape = RoundedCornerShape(24.sdp),
                    border = BorderStroke(2.sdp, colors.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.sdp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.font_preview_quote),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = FontFamily(typeface),
                                    lineHeight = 32.sp,
                                    fontSize = 20.sp
                                ),
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.basicMarquee()
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier
                                    .width(100.sdp)
                                    .padding(vertical = 12.sdp),
                                color = colors.onSurfaceVariant.copy(alpha = 0.3f),
                                thickness = 1.5.sdp
                            )
                            
                            Text(
                                text = stringResource(R.string.font_preview_numbers),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily(typeface),
                                    letterSpacing = 1.5.sp
                                ),
                                textAlign = TextAlign.Center,
                                color = colors.onSurfaceVariant,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(24.sdp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.sdp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.sdp),
                        shape = RoundedCornerShape(20.sdp),
                        border = BorderStroke(2.sdp, colors.outline)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.sdp),
                        shape = RoundedCornerShape(20.sdp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.add_font),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FontContent(
    fontOptions: List<FontOverlayOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onApply: () -> Unit,
    isApplying: Boolean,
    hasCustomFont: Boolean,
    customFontName: String,
    onPickFont: () -> Unit,
    onResetCustomFont: () -> Unit,
    listState: LazyListState,
    uiFontFamily: FontFamily
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(bottom = 100.sdp)
        ) {
            FontPreviewCard(fontOptions, selectedIndex, uiFontFamily)
            Spacer(Modifier.height(12.sdp))
            Text(
                text = stringResource(R.string.font_style),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = uiFontFamily),
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                modifier = Modifier.padding(horizontal = 24.sdp, vertical = 8.sdp)
            )
            FontStyleList(
                fontOptions,
                selectedIndex,
                onSelect,
                listState,
                uiFontFamily,
                hasCustomFont,
                customFontName
            )
            Spacer(Modifier.height(28.sdp))
            CustomFontSection(
                hasCustomFont = hasCustomFont,
                customFontName = customFontName,
                onPickFont = onPickFont,
                onResetCustomFont = onResetCustomFont,
                uiFontFamily = uiFontFamily
            )
            Spacer(Modifier.height(32.sdp))
        }

        ApplyButton(
            isApplying = isApplying,
            fontOptions = fontOptions,
            selectedIndex = selectedIndex,
            onApply = onApply,
            uiFontFamily = uiFontFamily,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.sdp, vertical = 32.sdp)
        )
    }
}

@Composable
private fun FontPreviewCard(
    fontOptions: List<FontOverlayOption>,
    selectedIndex: Int,
    uiFontFamily: FontFamily
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.sdp)
            .padding(horizontal = 24.sdp),
        color = colors.surfaceBright,
        shape = RoundedCornerShape(32.sdp),
        border = BorderStroke(2.sdp, colors.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(Modifier.fillMaxSize().padding(16.sdp), contentAlignment = Alignment.Center) {
            if (fontOptions.isNotEmpty() && selectedIndex < fontOptions.size) {
                FontPreviewLarge(
                    option = fontOptions[selectedIndex],
                    key = selectedIndex
                )
            }
        }
    }
}

@Composable
private fun FontStyleList(
    fontOptions: List<FontOverlayOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    listState: LazyListState,
    uiFontFamily: FontFamily,
    hasCustomFont: Boolean,
    customFontName: String
) {
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.sdp),
        contentPadding = PaddingValues(horizontal = 24.sdp),
        horizontalArrangement = Arrangement.spacedBy(12.sdp)
    ) {
        itemsIndexed(fontOptions) { index, option ->
            FontOptionCard(
                option = option,
                isSelected = index == selectedIndex,
                onClick = { onSelect(index) },
                uiFontFamily = uiFontFamily,
                hasCustomFont = hasCustomFont,
                customFontName = customFontName
            )
        }
    }
}

@Composable
private fun CustomFontSection(
    hasCustomFont: Boolean,
    customFontName: String,
    onPickFont: () -> Unit,
    onResetCustomFont: () -> Unit,
    uiFontFamily: FontFamily
) {
    val colors = MaterialTheme.colorScheme
    var showRebootDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.sdp),
        color = colors.surfaceBright,
        shape = RoundedCornerShape(28.sdp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.sdp),
            verticalArrangement = Arrangement.spacedBy(16.sdp)
        ) {
            CustomFontHeader(
                hasCustomFont = hasCustomFont,
                customFontName = customFontName,
                onPickFont = onPickFont,
                onResetCustomFont = onResetCustomFont,
                uiFontFamily = uiFontFamily
            )

            Button(
                onClick = { showRebootDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.sdp),
                shape = RoundedCornerShape(20.sdp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = stringResource(R.string.reboot_device),
                    modifier = Modifier.size(20.sdp)
                )
                Spacer(Modifier.width(8.sdp))
                Text(
                    stringResource(R.string.reboot_device),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = uiFontFamily),
                    fontWeight = FontWeight.SemiBold
                )
            }

            FooterCard(
                title = stringResource(R.string.reboot_required_custom_font_title),
                description = stringResource(R.string.reboot_required_custom_font_desc)
            )
        }
    }

    if (showRebootDialog) {
        ExpressiveDialog(
            showDialog = showRebootDialog,
            onDismiss = { showRebootDialog = false },
            title = stringResource(R.string.reboot_required_title),
            message = stringResource(R.string.reboot_required_message),
            confirmText = stringResource(R.string.reboot_device),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showRebootDialog = false
                ExternalFontInstaller.rebootDevice()
            }
        )
    }
}

@Composable
private fun CustomFontHeader(
    hasCustomFont: Boolean,
    customFontName: String,
    onPickFont: () -> Unit,
    onResetCustomFont: () -> Unit,
    uiFontFamily: FontFamily
) {
    val colors = MaterialTheme.colorScheme
    val default = stringResource(R.string.no_custom_font)

    val customFontDesc = if (hasCustomFont && customFontName.isNotEmpty()) customFontName else default

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.custom_font),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = uiFontFamily),
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(4.sdp))
            Text(
                text = customFontDesc,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = uiFontFamily),
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (hasCustomFont) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.sdp)) {
                FilledTonalIconButton(
                    onClick = onPickFont,
                    modifier = Modifier.size(48.sdp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.change_custom_font)
                    )
                }

                FilledTonalIconButton(
                    onClick = onResetCustomFont,
                    modifier = Modifier.size(48.sdp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.errorContainer,
                        contentColor = colors.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.reset_custom_font_content_desc)
                    )
                }
            }
        } else {
            FilledTonalIconButton(
                onClick = onPickFont,
                modifier = Modifier.size(48.sdp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_custom_font)
                )
            }
        }
    }
}

@Composable
private fun ApplyButton(
    isApplying: Boolean,
    fontOptions: List<FontOverlayOption>,
    selectedIndex: Int,
    onApply: () -> Unit,
    uiFontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onApply,
        modifier = modifier
            .fillMaxWidth()
            .height(68.sdp),
        enabled = !isApplying && selectedIndex < fontOptions.size && !fontOptions[selectedIndex].isActive,
        shape = RoundedCornerShape(24.sdp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceContainerHighest,
            disabledContentColor = colors.onSurfaceVariant
        )
    ) {
        if (isApplying) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.sdp),
                color = colors.onPrimary,
                strokeWidth = 4.sdp
            )
        } else {
            Text(
                text = stringResource(if (fontOptions.getOrNull(selectedIndex)?.isActive == true) R.string.applied else R.string.apply),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = uiFontFamily),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FontPreviewLarge(
    option: FontOverlayOption,
    key: Int
) {
    val colors = MaterialTheme.colorScheme
    
    key(key) {
        var visible by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            delay(50)
            visible = true
        }
        
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                initialOffsetY = { -20 }
            ),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = stringResource(R.string.font_preview_quote),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily(option.bodyFont),
                        lineHeight = 32.sp,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 8.sdp).basicMarquee()
                )
                HorizontalDivider(
                    modifier = Modifier
                        .width(120.sdp)
                        .padding(vertical = 8.sdp),
                    color = colors.onSurfaceVariant.copy(alpha = 0.3f),
                    thickness = 1.5.sdp
                )
                Text(
                    text = stringResource(R.string.font_preview_numbers),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily(option.bodyFont),
                        letterSpacing = 1.5.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun FontOptionCard(
    option: FontOverlayOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    uiFontFamily: FontFamily,
    hasCustomFont: Boolean,
    customFontName: String
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val displayLabel = if (option.label.contains("default", ignoreCase = true) && hasCustomFont) {
        customFontName.ifEmpty { stringResource(R.string.custom) }
    } else {
        option.label
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier
            .width(100.sdp)
            .fillMaxHeight()
            .scale(scale),
        color = if (isSelected) colors.primaryContainer else colors.surfaceBright,
        shape = RoundedCornerShape(24.sdp),
        border = BorderStroke(
            width = if (isSelected) 2.5.sdp else 2.sdp,
            color = if (isSelected) colors.primary else colors.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.sdp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.font_preview_aa),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily(option.bodyFont),
                            fontSize = 42.sp
                        ),
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily(option.bodyFont),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )

                    if (option.isActive) {
                        Spacer(modifier = Modifier.height(4.sdp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.active_content_desc),
                            modifier = Modifier.size(16.sdp),
                            tint = colors.primary
                        )
                    }
                }
            }
        }
    }
}
