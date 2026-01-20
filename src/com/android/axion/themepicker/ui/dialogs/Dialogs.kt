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
package com.android.axion.themepicker.ui.dialogs

import android.content.Context
import android.graphics.Color as GraphicsColor
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.android.axion.themepicker.R
import com.android.axion.themepicker.data.model.ThemeStyle
import androidx.compose.material3.MaterialTheme
import com.android.axion.themepicker.utils.colors.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import kotlin.math.*

@Composable
fun StylePickerDialog(
    currentStyle: ThemeStyle,
    onDismiss: () -> Unit,
    onStyleSelected: (ThemeStyle) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(320.dp)
            ) {
                Text(
                    text = stringResource(R.string.theme_style_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeStyle.values().forEach { style ->
                        val isSelected = style == currentStyle
                        Surface(
                            onClick = { onStyleSelected(style) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) 
                                colors.primaryContainer 
                            else 
                                colors.surface,
                            border = if (isSelected) 
                                null 
                            else 
                                BorderStroke(
                                    1.dp, 
                                    colors.outline
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = style.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected)
                                        colors.onPrimaryContainer
                                    else
                                        colors.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedColor by remember { mutableStateOf(initialColor) }
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0.5f) }
    var brightness by remember { mutableStateOf(0.5f) }

    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        GraphicsColor.colorToHSV(initialColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        selectedColor = Color.hsv(hue, saturation, brightness)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(320.dp)
            ) {
                Text(
                    text = stringResource(R.string.choose_seed_color),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(selectedColor)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.hue),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HuePicker(
                    hue = hue,
                    onHueChange = { newHue ->
                        hue = newHue
                        selectedColor = Color.hsv(hue, saturation, brightness)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.saturation),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ColorSlider(
                    value = saturation,
                    onValueChange = { newsaturation ->
                        saturation = newsaturation
                        selectedColor = Color.hsv(hue, saturation, brightness)
                    },
                    startColor = Color.hsv(hue, 0f, brightness),
                    endColor = Color.hsv(hue, 1f, brightness)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.brightness),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ColorSlider(
                    value = brightness,
                    onValueChange = { newValue ->
                        brightness = newValue
                        selectedColor = Color.hsv(hue, saturation, brightness)
                    },
                    startColor = Color.hsv(hue, saturation, 0f),
                    endColor = Color.hsv(hue, saturation, 1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(
                        R.string.color_info_format,
                        String.format("%06X", 0xFFFFFF and selectedColor.toArgb()),
                        saturation,
                        brightness
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(selectedColor) }) {
                        Text(stringResource(R.string.apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun HuePicker(hue: Float, onHueChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newHue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                        onHueChange(newHue)
                    }
                }
        ) {
            val gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.hsv(0f, 1f, 1f),
                    Color.hsv(60f, 1f, 1f),
                    Color.hsv(120f, 1f, 1f),
                    Color.hsv(180f, 1f, 1f),
                    Color.hsv(240f, 1f, 1f),
                    Color.hsv(300f, 1f, 1f),
                    Color.hsv(360f, 1f, 1f)
                )
            )
            drawRect(gradient)

            val selectorX = (hue / 360f) * size.width
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(selectorX, size.height / 2),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
private fun ColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    startColor: Color,
    endColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                }
        ) {
            val gradient = Brush.horizontalGradient(
                colors = listOf(startColor, endColor)
            )
            drawRect(gradient)

            val selectorX = value * size.width
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(selectorX, size.height / 2),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun WallpaperColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    
    var nativeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                 val wallpaperManager = WallpaperManager.getInstance(context)
                 val drawable = wallpaperManager.drawable
                 if (drawable != null) {
                     val bmp = (drawable as? BitmapDrawable)?.bitmap 
                         ?: run {
                            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1080
                            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1920
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            bitmap
                         }
                     
                     val maxDim = 1000
                     val scale = if (bmp.width > maxDim || bmp.height > maxDim) {
                         val ratio = min(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                         Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                     } else bmp
                     
                     nativeBitmap = scale
                     imageBitmap = scale.asImageBitmap()
                 }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.pick_from_wallpaper),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.Start)
                )

                if (imageBitmap != null && nativeBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .aspectRatio(nativeBitmap!!.width.toFloat() / nativeBitmap!!.height.toFloat())
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    isDragging = true
                                    touchPosition = down.position
                                    
                                    val updateColor = { pos: Offset ->
                                        val bmp = nativeBitmap!!
                                        val xCenter = (pos.x / size.width * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                                        val yCenter = (pos.y / size.height * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                                        
                                        var rSum = 0
                                        var gSum = 0
                                        var bSum = 0
                                        var count = 0
                                        val radius = 5
                                        
                                        for (dX in -radius..radius) {
                                            for (dY in -radius..radius) {
                                                val x = xCenter + dX
                                                val y = yCenter + dY
                                                
                                                if (x in 0 until bmp.width && y in 0 until bmp.height) {
                                                    val pixel = bmp.getPixel(x, y)
                                                    rSum += GraphicsColor.red(pixel)
                                                    gSum += GraphicsColor.green(pixel)
                                                    bSum += GraphicsColor.blue(pixel)
                                                    count++
                                                }
                                            }
                                        }
                                        
                                        if (count > 0) {
                                            selectedColor = Color(
                                                red = rSum / count,
                                                green = gSum / count,
                                                blue = bSum / count
                                            )
                                        }
                                    }
                                    
                                    updateColor(down.position)
                                    
                                    var drag = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                        change.consume()
                                    }
                                    
                                    if (drag != null) {
                                         var pointer = drag.id
                                         while (true) {
                                             val event = awaitPointerEvent()
                                             val dragChange = event.changes.firstOrNull { it.id == pointer } ?: break
                                             if (dragChange.pressed != true) break
                                             
                                             touchPosition = dragChange.position
                                             updateColor(dragChange.position)
                                             dragChange.consume()
                                         }
                                    }
                                    
                                    isDragging = false
                                    touchPosition = null
                                }
                            }
                    ) {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        if (isDragging && touchPosition != null && selectedColor != null) {
                            val magnifierSize = 80.dp
                            val magnifierSizePx = with(LocalDensity.current) { magnifierSize.toPx() }
                            
                            val xPos = touchPosition!!.x
                            val yPos = touchPosition!!.y - magnifierSizePx / 1.2f 
                            
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((xPos - magnifierSizePx / 2).toInt(), (yPos - magnifierSizePx / 2).toInt()) }
                                    .size(magnifierSize)
                                    .shadow(elevation = 8.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .background(selectedColor!!)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Text(
                            text = stringResource(R.string.selected_color_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(selectedColor ?: Color.Transparent)
                                .border(1.dp, colors.outline, CircleShape)
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (selectedColor != null) {
                            Text(
                                text = "#${String.format("%06X", 0xFFFFFF and selectedColor!!.toArgb())}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { selectedColor?.let { onColorSelected(it) } },
                            enabled = selectedColor != null
                        ) {
                            Text(stringResource(R.string.apply))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
