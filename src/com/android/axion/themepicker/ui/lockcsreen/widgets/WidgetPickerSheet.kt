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

package com.android.axion.themepicker.ui.lockscreen.widgets

import android.appwidget.AppWidgetProviderInfo
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.components.CommonBottomSheet

@Composable
fun WidgetPickerBottomSheet(
    visible: Boolean,
    widgets: List<GridWidgetItem>,
    onDismiss: () -> Unit,
    onSelect: (GridWidgetItem) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    var selectedProvider by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedPackage by remember { mutableStateOf<String?>(null) }

    val allGroups = remember { getGroupedWidgetProviders(context) }
    val filteredGroups by
        remember(searchQuery, allGroups) {
            derivedStateOf {
                if (searchQuery.isBlank()) allGroups
                else searchWidgetProviders(allGroups, searchQuery)
            }
        }

    val titleText =
        if (selectedProvider != null) selectedProvider!!.loadLabel(context.packageManager)
        else stringResource(R.string.pick_widgets)

    CommonBottomSheet(
        visible = visible,
        title = titleText,
        heightFraction = 0.75f,
        surfaceColor = if (selectedProvider != null) colors.surfaceContainerHigh else null,
        onDismiss = {
            selectedProvider = null
            searchQuery = ""
            expandedPackage = null
            onDismiss()
        },
    ) {
        AnimatedContent(
            targetState = selectedProvider,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "picker_stage",
        ) { provider ->
            if (provider != null) {
                WidgetSizePicker(
                    provider = provider,
                    widgets = widgets,
                    onSelect = onSelect,
                    onDismiss = {
                        selectedProvider = null
                        searchQuery = ""
                        expandedPackage = null
                        onDismiss()
                    },
                    onBack = { selectedProvider = null },
                )
            } else {
                WidgetBrowser(
                    groups = filteredGroups,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    expandedPackage = expandedPackage,
                    onToggleExpand = { pkg ->
                        expandedPackage = if (expandedPackage == pkg) null else pkg
                    },
                    isSearching = searchQuery.isNotBlank(),
                    onSelectWidget = { info -> selectedProvider = info },
                )
            }
        }
    }
}

@Composable
private fun WidgetBrowser(
    groups: List<WidgetAppGroup>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    expandedPackage: String?,
    onToggleExpand: (String) -> Unit,
    isSearching: Boolean,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        WidgetSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(8.dp))

        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Widgets,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text =
                            if (isSearching) stringResource(R.string.no_results)
                            else stringResource(R.string.no_widgets_available),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                groups.forEachIndexed { groupIndex, group ->
                    val isExpanded =
                        expandedPackage == group.packageName || (isSearching && groups.size == 1)
                    val isFirst = groupIndex == 0
                    val isLast = groupIndex == groups.size - 1

                    item(key = "header_${group.packageName}") {
                        WidgetAppHeader(
                            group = group,
                            isExpanded = isExpanded,
                            isFirst = isFirst,
                            isLast = isLast && !isExpanded,
                            onClick = { onToggleExpand(group.packageName) },
                        )
                    }

                    item(key = "content_${group.packageName}") {
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter =
                                expandVertically(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                                ) +
                                    fadeIn(
                                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
                                    ),
                            exit =
                                shrinkVertically(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                                ) +
                                    fadeOut(
                                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
                                    ),
                        ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .background(
                                            color = colors.surfaceContainerHigh,
                                            shape =
                                                RoundedCornerShape(
                                                    topStart = 0.dp,
                                                    topEnd = 0.dp,
                                                    bottomStart = if (isLast) 20.dp else 4.dp,
                                                    bottomEnd = if (isLast) 20.dp else 4.dp,
                                                ),
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                group.widgets.forEach { meta ->
                                    WidgetCard(meta = meta, onClick = { onSelectWidget(meta.info) })
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun WidgetSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.height(48.dp).focusRequester(focusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier.fillMaxSize()
                        .background(colors.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_widgets),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun WidgetAppHeader(
    group: WidgetAppGroup,
    isExpanded: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    val chevronRotation by
        animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "chevron_rotation",
        )

    val bgColor by
        animateColorAsState(
            targetValue = if (isExpanded) colors.surfaceContainerHigh else colors.surfaceContainer,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "header_bg",
        )

    val shape =
        RoundedCornerShape(
            topStart = if (isFirst) 20.dp else 4.dp,
            topEnd = if (isFirst) 20.dp else 4.dp,
            bottomStart = if (isLast && !isExpanded) 20.dp else 4.dp,
            bottomEnd = if (isLast && !isExpanded) 20.dp else 4.dp,
        )

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (group.appIcon != null) {
            Image(
                bitmap = group.appIcon.asImageBitmap(),
                contentDescription = group.appLabel,
                modifier = Modifier.size(28.dp).clip(MaterialTheme.shapes.small),
            )
        } else {
            Box(
                modifier =
                    Modifier.size(28.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Widgets,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Text(
            text = group.appLabel,
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = "${group.widgets.size}",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )

        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = chevronRotation },
        )
    }
}

@Composable
private fun WidgetCard(meta: WidgetProviderMeta, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val info = meta.info
    val density = context.resources.displayMetrics.density

    val minWidth = info.minWidth
    val minHeight = info.minHeight
    val cellWidth = 74
    val spanX = ((minWidth / density).toInt() / cellWidth + 1).coerceIn(1, 4)
    val spanY = ((minHeight / density).toInt() / cellWidth + 1).coerceIn(1, 2)
    val spanString = "${spanX}\u00A0\u00D7\u00A0${spanY}"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(colors.surfaceBright),
                contentAlignment = Alignment.Center,
            ) {
                WidgetPreviewContent(info = info)
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = meta.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = colors.surfaceContainerHigh,
                ) {
                    Text(
                        text = spanString,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            if (!meta.description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WidgetPreviewContent(info: AppWidgetProviderInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var previewLayoutFailed by remember(info) { mutableStateOf(false) }
    val usePreviewLayout = info.previewLayout != 0 && !previewLayoutFailed

    if (usePreviewLayout) {
        var containerWidthPx by remember { mutableIntStateOf(0) }
        var containerHeightPx by remember { mutableIntStateOf(0) }

        AndroidView(
            factory = { ctx ->
                WidgetPreviewHostView(ctx).apply {
                    try {
                        setAppWidget(-1, info)

                        val previewViews =
                            RemoteViews(info.provider.packageName, info.previewLayout)
                        updateAppWidget(previewViews)
                    } catch (_: Exception) {
                        previewLayoutFailed = true
                    }
                    post {
                        if (childCount == 0) {
                            previewLayoutFailed = true
                        }
                    }
                }
            },
            modifier =
                modifier.fillMaxSize().padding(8.dp).onSizeChanged { size ->
                    containerWidthPx = size.width
                    containerHeightPx = size.height
                },
            update = { view ->
                if (containerWidthPx > 0 && containerHeightPx > 0) {
                    view.setContainerSizePx(containerWidthPx, containerHeightPx)
                    view.requestLayout()
                }
            },
        )
    } else {
        BitmapWidgetPreview(info = info, modifier = modifier)
    }
}

@Composable
private fun BitmapWidgetPreview(info: AppWidgetProviderInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dpi = context.resources.displayMetrics.densityDpi

    val previewDrawable = remember(info) { info.loadPreviewImage(context, dpi) }
    val bitmap = remember(info) { loadWidgetPreviewBitmap(context, info) }

    if (previewDrawable != null && bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = info.loadLabel(context.packageManager),
            modifier = modifier.fillMaxSize().padding(8.dp),
            contentScale = ContentScale.Fit,
        )
    } else if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = info.loadLabel(context.packageManager),
            modifier = modifier.size(48.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Icon(
            Icons.Default.Widgets,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(48.dp),
        )
    }
}

@Composable
private fun WidgetSizePicker(
    provider: AppWidgetProviderInfo,
    widgets: List<GridWidgetItem>,
    onSelect: (GridWidgetItem) -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val noSpaceMsg = stringResource(R.string.no_space_available)
    val providerFlat = provider.provider.flattenToString()

    val label = remember(provider) { provider.loadLabel(context.packageManager) }
    val description =
        remember(provider) {
            try {
                provider.loadDescription(context)?.toString()
            } catch (_: Exception) {
                null
            }
        }

    val sizeRows = listOf(listOf(1 to 1, 2 to 1, 4 to 1), listOf(1 to 2, 2 to 2, 4 to 2))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(100.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                WidgetPreviewContent(info = provider)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                sizeRows.forEach { rowSizes ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowSizes.forEach { (spanX, spanY) ->
                            val canFit = findAvailablePosition(widgets, spanX, spanY) != null

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier =
                                    Modifier.clip(MaterialTheme.shapes.large)
                                        .clickable(
                                            indication = null,
                                            interactionSource =
                                                remember { MutableInteractionSource() },
                                        ) {
                                            val pos = findAvailablePosition(widgets, spanX, spanY)
                                            if (pos != null) {
                                                onSelect(
                                                    GridWidgetItem(
                                                        provider = providerFlat,
                                                        cellX = pos.first,
                                                        cellY = pos.second,
                                                        spanX = spanX,
                                                        spanY = spanY,
                                                    )
                                                )
                                                onDismiss()
                                            } else {
                                                Toast.makeText(
                                                        context,
                                                        noSpaceMsg,
                                                        Toast.LENGTH_SHORT,
                                                    )
                                                    .show()
                                            }
                                        }
                                        .padding(8.dp),
                            ) {
                                val previewW = (32 * spanX).dp
                                val previewH = (32 * spanY).dp
                                Box(
                                    modifier =
                                        Modifier.size(width = previewW, height = previewH)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(
                                                if (canFit) colors.primaryContainer
                                                else colors.surfaceContainerHigh
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint =
                                            if (canFit) colors.onPrimaryContainer
                                            else colors.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${spanX}\u00D7${spanY}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color =
                                        if (canFit) colors.onSurface
                                        else colors.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier =
                Modifier.align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHighest)
                    .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colors.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
