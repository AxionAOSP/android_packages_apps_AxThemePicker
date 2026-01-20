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
package com.android.axion.themepicker.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.android.axion.themepicker.R
import com.android.axion.themepicker.data.model.WallpaperInfo
import com.android.axion.themepicker.data.model.WallpaperSettings
import com.android.axion.themepicker.ui.mainscreen.StandaloneWallpaperApplyScreen
import com.android.axion.themepicker.ui.theme.AxTheme
import com.android.axion.themepicker.utils.wallpaper.decodeSampledBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

class WallpaperSetActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WallpaperSetActivity"
    }

    private var interceptedBitmap by mutableStateOf<Bitmap?>(null)
    private var isLoadingImage by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleImageIntent()

        setContent {
            AxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        isLoadingImage -> LoadingScreen()
                        interceptedBitmap != null -> {
                            StandaloneWallpaperApplyScreen(
                                wallpaper = WallpaperInfo(
                                    id = "shared_image",
                                    title = stringResource(R.string.shared_image),
                                    drawableRes = -1
                                ),
                                settings = WallpaperSettings(
                                    lockscreen = true,
                                    homescreen = true
                                ),
                                bitmap = interceptedBitmap,
                                onBackClick = { finish() },
                                onApplyComplete = { finish() }
                            )
                        }
                        else -> ErrorScreen()
                    }
                }
            }
        }
    }

    private fun handleImageIntent() {
        when (intent?.action) {
            Intent.ACTION_SET_WALLPAPER,
            Intent.ACTION_ATTACH_DATA,
            Intent.ACTION_GET_CONTENT -> {
                val uri: Uri? = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
                
                if (uri != null) {
                    Log.d(TAG, "Processing image URI: $uri")
                    loadImageFromUri(uri)
                } else {
                    Log.e(TAG, "No URI found in intent")
                    Toast.makeText(this, getString(R.string.no_image_provided), Toast.LENGTH_SHORT).show()
                    isLoadingImage = false
                }
            }
            else -> {
                Log.e(TAG, "Unsupported intent action: ${intent?.action}")
                isLoadingImage = false
            }
        }
    }

    private fun loadImageFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val context = this@WallpaperSetActivity
            try {
                val bitmap = decodeSampledBitmapFromUri(context, uri)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        interceptedBitmap = bitmap
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.failed_to_load_image),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    isLoadingImage = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        getString(R.string.error_loading_image, e.message ?: getString(R.string.unknown)),
                        Toast.LENGTH_SHORT
                    ).show()
                    isLoadingImage = false
                }
            }
        }
    }

    @Composable
    private fun LoadingScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun ErrorScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.unable_to_load_image),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { finish() }) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
