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
package com.android.axion.themepicker.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.axion.themepicker.R

sealed class NavigationDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Wallpaper : NavigationDestination(
        route = "wallpaper",
        labelRes = R.string.nav_wallpaper,
        selectedIcon = Icons.Filled.Wallpaper,
        unselectedIcon = Icons.Outlined.Wallpaper
    )
    
    object Style : NavigationDestination(
        route = "style",
        labelRes = R.string.nav_style,
        selectedIcon = Icons.Filled.Palette,
        unselectedIcon = Icons.Outlined.Palette
    )
    
    object Lockscreen : NavigationDestination(
        route = "lockscreen",
        labelRes = R.string.nav_lockscreen,
        selectedIcon = Icons.Filled.Lock,
        unselectedIcon = Icons.Outlined.Lock
    )
    
    companion object {
        val destinations = listOf(Wallpaper, Style, Lockscreen)
    }
}
