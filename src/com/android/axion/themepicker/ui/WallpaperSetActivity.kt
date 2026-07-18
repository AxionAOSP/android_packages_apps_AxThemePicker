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

package com.android.axion.themepicker.ui

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.lifecycle.lifecycleScope
import com.android.axion.themepicker.R
import com.android.axion.themepicker.ui.theme.AxTheme
import com.android.axion.themepicker.ui.wallpaperset.WallpaperCropScreen
import com.android.axion.themepicker.ui.wallpaperset.adjustCropRect
import com.android.axion.themepicker.utils.wallpaper.toCompressedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WallpaperSetActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WallpaperSetActivity"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageUri: Uri? = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)

        if (imageUri == null) {
            Log.e(TAG, "No URI found in intent; finishing.")
            Toast.makeText(this, getString(R.string.no_image_provided), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val isUriPermissionGranted =
            checkUriPermission(
                imageUri,
                Binder.getCallingPid(),
                Binder.getCallingUid(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            ) == PackageManager.PERMISSION_GRANTED

        if (!isUriPermissionGranted && !isReadMediaPermissionGranted()) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                PERMISSION_REQUEST_CODE,
            )
            return
        }

        showCropScreen(imageUri)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val isGranted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                Log.e(TAG, "Permission denied; finishing.")
                finish()
                return
            }

            val imageUri = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (imageUri != null) {
                showCropScreen(imageUri)
            } else {
                finish()
            }
        }
    }

    private fun showCropScreen(uri: Uri) {
        setContent {
            AxTheme {
                WallpaperCropScreen(
                    imageUri = uri,
                    onApply = { imageUri, cropRect, multiCropHints, flags ->
                        applyWallpaper(imageUri, cropRect, multiCropHints, flags)
                    },
                    onApplyBitmap = { bitmap, flags -> applyWallpaperBitmap(bitmap, flags) },
                    onCancel = { finish() },
                )
            }
        }
    }

    private fun applyWallpaper(
        uri: Uri,
        cropRect: Rect,
        multiCropHints: Map<Point, Rect>?,
        flags: Int,
    ) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val wm = WallpaperManager.getInstance(this@WallpaperSetActivity)

                    if (multiCropHints != null && multiCropHints.isNotEmpty()) {

                        try {
                            Log.d(
                                TAG,
                                "Applying with multi-crop: " +
                                    "${multiCropHints.size} display hints, flags=$flags",
                            )
                            contentResolver.openInputStream(uri)?.use { stream ->
                                wm.setStreamWithCrops(stream, multiCropHints, true, flags)
                            } ?: throw Exception("Failed to open image stream")
                        } catch (e: NoSuchMethodError) {

                            Log.w(
                                TAG,
                                "setStreamWithCrops not available, " + "falling back to setStream",
                            )
                            val adjustedCropRect = Rect(cropRect).apply {
                                adjustCropRect(this@WallpaperSetActivity, this, zoomIn = false)
                            }
                            contentResolver.openInputStream(uri)?.use { stream ->
                                wm.setStream(stream, adjustedCropRect, true, flags)
                            } ?: throw Exception("Failed to open image stream")
                        }
                    } else {

                        Log.d(TAG, "Applying: cropHint=$cropRect, flags=$flags")
                        val adjustedCropRect = Rect(cropRect).apply {
                            adjustCropRect(this@WallpaperSetActivity, this, zoomIn = false)
                        }
                        contentResolver.openInputStream(uri)?.use { stream ->
                            wm.setStream(stream, adjustedCropRect, true, flags)
                        } ?: throw Exception("Failed to open image stream")
                    }
                }

                Toast.makeText(
                        this@WallpaperSetActivity,
                        getString(R.string.wallpaper_set_success),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                goHome()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set wallpaper", e)
                Toast.makeText(
                        this@WallpaperSetActivity,
                        getString(R.string.failed_to_load_image),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                goHome()
            }
        }
    }

    private fun applyWallpaperBitmap(bitmap: Bitmap, flags: Int) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val wm = WallpaperManager.getInstance(this@WallpaperSetActivity)
                    Log.d(TAG, "Applying fit-mode bitmap=${bitmap.width}x${bitmap.height}, flags=$flags")
                    wm.setStream(bitmap.toCompressedStream(), null, false, flags)
                }
                Toast.makeText(
                        this@WallpaperSetActivity,
                        getString(R.string.wallpaper_set_success),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                goHome()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set fit-mode wallpaper", e)
                Toast.makeText(
                        this@WallpaperSetActivity,
                        getString(R.string.failed_to_load_image),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                goHome()
            }
        }
    }

    private fun goHome() = restartApp()

    private fun isReadMediaPermissionGranted(): Boolean {
        return packageManager.checkPermission(Manifest.permission.READ_MEDIA_IMAGES, packageName) ==
            PackageManager.PERMISSION_GRANTED
    }
}
