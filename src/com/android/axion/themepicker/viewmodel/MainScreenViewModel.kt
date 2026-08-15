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

package com.android.axion.themepicker.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.axion.themepicker.data.model.Screen
import com.android.axion.themepicker.data.model.Screen.EntryPoint
import com.android.axion.themepicker.data.model.WallpaperInfo
import com.android.axion.themepicker.utils.wallpaper.loadWallpapers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainScreenViewModel : ViewModel() {

    private val _wallpapers = MutableStateFlow<List<WallpaperInfo>>(emptyList())
    val wallpapers: StateFlow<List<WallpaperInfo>> = _wallpapers

    private val _selectedTab = MutableStateFlow(1)
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Main)
    val currentScreen: StateFlow<Screen> = _currentScreen

    private val _screenStack = mutableListOf<Screen>()

    private val _isNavigatingBack = MutableStateFlow(false)
    val isNavigatingBack: StateFlow<Boolean> = _isNavigatingBack

    fun initialize(context: Context) {
        if (_wallpapers.value.isNotEmpty()) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = withContext(Dispatchers.IO) { loadWallpapers(appContext) }
            _wallpapers.value = loaded
            _isLoading.value = false
        }
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun navigateTo(screen: Screen) {
        _isNavigatingBack.value = false
        _screenStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun goBack() {
        _isNavigatingBack.value = true

        if (_screenStack.isNotEmpty()) {
            _currentScreen.value = _screenStack.removeAt(_screenStack.lastIndex)
        } else {
            _isNavigatingBack.value = false
            _currentScreen.value = Screen.Main
        }
    }

    fun resetToMain() {
        _isNavigatingBack.value = false
        _currentScreen.value = Screen.Main
        _screenStack.clear()
        Log.d("MainScreenViewModel", "reset to main!")
    }

    fun onOpenColorsSettings() {
        navigateTo(Screen.ColorsSettings)
    }

    fun onOpenIconShapes() {
        navigateTo(Screen.IconShapes)
    }

    fun onOpenFonts() {
        navigateTo(Screen.Fonts)
    }

    fun onOpenGallery(targetFlags: Int = 0) {
        navigateTo(Screen.WallpaperGallery(targetFlags))
    }

    fun onOpenWallpaperEffects() {
        navigateTo(Screen.WallpaperEffects)
    }

    var pendingPreviewBitmap: Bitmap? = null
        private set

    var pendingPreviewCropHints: Map<Point, Rect> = emptyMap()
        private set

    fun onOpenWallpaperCrop(sourceUri: Uri? = null, drawableRes: Int = 0, targetFlags: Int = 0) {
        navigateTo(
            Screen.WallpaperCrop(
                sourceUri = sourceUri,
                drawableRes = drawableRes,
                targetFlags = targetFlags,
            )
        )
    }

    fun onCropCompleted(
        bitmap: Bitmap,
        cropHints: Map<Point, Rect>,
        targetFlags: Int = 0,
    ) {
        pendingPreviewBitmap = bitmap
        pendingPreviewCropHints = cropHints
        navigateTo(Screen.WallpaperPreview(targetFlags = targetFlags))
    }

    fun returnToLockscreenPreview() {
        val lockscreenIndex = _screenStack.indexOfLast { it is Screen.Lockscreen }
        if (lockscreenIndex < 0) {
            resetToMain()
            return
        }
        _isNavigatingBack.value = true
        _currentScreen.value = _screenStack[lockscreenIndex]
        _screenStack.subList(lockscreenIndex, _screenStack.size).clear()
        pendingPreviewBitmap = null
        pendingPreviewCropHints = emptyMap()
    }

    fun onOpenLockscreenPreview(
        wallpaper: WallpaperInfo? = null,
        entryPoint: EntryPoint = EntryPoint.DEFAULT,
    ) {
        navigateTo(Screen.Lockscreen(wallpaper, entryPoint))
        Log.d("MainScreenViewModel", "entryPoint=$entryPoint")
    }
}
