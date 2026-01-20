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
package com.android.axion.themepicker.ui.expressive

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.theme.LocalAdaptiveLayoutInfo
import com.android.axion.themepicker.ui.theme.LocalExpressiveDesign
import com.android.axion.themepicker.ui.theme.ExpressiveScale

@Composable
fun ExpressiveHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector = Icons.Default.Refresh,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val design = LocalExpressiveDesign.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    
    val horizontalPadding = if (layoutInfo.isTablet) 24.dp else 8.dp
    val topPadding = if (layoutInfo.isTablet) 24.dp else 48.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = horizontalPadding, top = topPadding, end = horizontalPadding, bottom = 16.dp)
        ) {
            ExpressiveIconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
            }

            if (onActionClick != null) {
                ExpressiveIconButton(
                    onClick = onActionClick,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enabled = enabled
                ) {
                    Icon(actionIcon, contentDescription = stringResource(R.string.nav_action))
                }
            }

            Column(
                modifier = Modifier.align(
                    if (layoutInfo.isTablet) Alignment.CenterStart else Alignment.Center
                ).then(
                    if (layoutInfo.isTablet) Modifier.padding(start = 56.dp) else Modifier
                ),
                horizontalAlignment = if (layoutInfo.isTablet) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    content: @Composable () -> Unit
) {
    val design = LocalExpressiveDesign.current
    
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(design.shapes.buttonCorner),
        enabled = enabled
    ) {
        content()
    }
}

@Composable
fun ExpressiveLargeHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val design = LocalExpressiveDesign.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (layoutInfo.isTablet) design.spacing.screenPaddingTablet else design.spacing.screenPadding,
                vertical = design.spacing.medium
            ),
        verticalArrangement = Arrangement.spacedBy(design.spacing.extraSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(design.spacing.small)
        ) {
            if (onBackClick != null) {
                ExpressiveIconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
        
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = if (onBackClick != null) 56.dp else 0.dp)
            )
        }
    }
}
