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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TRANSITION_SETTLE_MS = 400L
private const val RENDER_BUFFER_MS = 100L

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeferredScreen(
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    loadingContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    loadingIndicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    content: @Composable () -> Unit,
) {
    var contentComposed by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(TRANSITION_SETTLE_MS)
        contentComposed = true
    }

    LaunchedEffect(contentComposed) {
        if (contentComposed) {
            delay(RENDER_BUFFER_MS)
            showOverlay = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (contentComposed) {
            content()
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(backgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator(
                    modifier = Modifier.size(48.dp),
                    containerColor = loadingContainerColor,
                    indicatorColor = loadingIndicatorColor,
                )
            }
        }
    }
}
