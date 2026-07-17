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

package com.android.axion.themepicker.ui.components

import android.app.WallpaperManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.themepicker.R

@Composable
internal fun WallpaperTargetDialog(
    colors: ColorScheme,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = colors.primary)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_wallpaper_on)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onSelect(WallpaperManager.FLAG_SYSTEM) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = buttonColors,
                ) {
                    Text(
                        text = stringResource(R.string.home_screen),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(
                    onClick = { onSelect(WallpaperManager.FLAG_LOCK) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = buttonColors,
                ) {
                    Text(
                        text = stringResource(R.string.lock_screen_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(
                    onClick = {
                        onSelect(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = buttonColors,
                ) {
                    Text(
                        text = stringResource(R.string.home_lock_both),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, colors = buttonColors) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        containerColor = colors.surfaceContainerHigh,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
    )
}
