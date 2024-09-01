/*
 * Copyright (C) 2024 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.panpf.sketch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize

/**
 * Get window container size
 *
 * @see com.github.panpf.sketch.compose.core.android.test.AsyncImageStateAndroidTest.testGetWindowContainerSize
 */
@Composable
actual fun getWindowContainerSize(): IntSize {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    return remember(displayMetrics) {
        IntSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
}