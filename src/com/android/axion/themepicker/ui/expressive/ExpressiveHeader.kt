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

package com.android.axion.themepicker.ui.expressive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.theme.LocalAdaptiveLayoutInfo

@Composable
fun ExpressiveHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector = Icons.Default.Refresh,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val horizontalPadding = if (layoutInfo.isTablet) 24.dp else 8.dp

    Surface(modifier = modifier.fillMaxWidth(), color = Color.Transparent) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = horizontalPadding,
                        top = 8.dp,
                        end = horizontalPadding,
                        bottom = 16.dp,
                    )
        ) {
            ExpressiveIconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }

            if (onActionClick != null) {
                ExpressiveIconButton(
                    onClick = onActionClick,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enabled = enabled,
                ) {
                    Icon(actionIcon, contentDescription = "Action")
                }
            }

            Column(
                modifier =
                    Modifier.align(
                            if (layoutInfo.isTablet) Alignment.CenterStart else Alignment.Center
                        )
                        .then(
                            if (layoutInfo.isTablet) Modifier.padding(start = 56.dp) else Modifier
                        ),
                horizontalAlignment =
                    if (layoutInfo.isTablet) Alignment.Start else Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    ) {
        content()
    }
}

@Composable
fun ExpressiveLargeHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val layoutInfo = LocalAdaptiveLayoutInfo.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = if (layoutInfo.isTablet) 24.dp else 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onBackClick != null) {
                ExpressiveIconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurface,
            )
        }

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = if (onBackClick != null) 56.dp else 0.dp),
            )
        }
    }
}
