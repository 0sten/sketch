@file:Suppress("DEPRECATION")

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

package com.github.panpf.sketch.core.common.test.request.internal

import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.CachePolicy.WRITE_ONLY
import com.github.panpf.sketch.images.ResourceImages
import com.github.panpf.sketch.request.Depth
import com.github.panpf.sketch.request.Depth.MEMORY
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.ImageResult
import com.github.panpf.sketch.request.internal.RequestExecutor
import com.github.panpf.sketch.resize.FixedSizeResolver
import com.github.panpf.sketch.resize.SizeResolver
import com.github.panpf.sketch.test.singleton.getTestContextAndSketch
import com.github.panpf.sketch.test.utils.getTestContextAndNewSketch
import com.github.panpf.sketch.util.screenSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestExecutorTest {

    @Test
    fun testErrorUri() = runTest {
        val (context, sketch) = getTestContextAndSketch()

        withContext(Dispatchers.Main) {
            RequestExecutor().execute(
                sketch,
                ImageRequest(context, ResourceImages.jpeg.uri),
                false
            ).apply {
                assertTrue(this is ImageResult.Success)
            }

            RequestExecutor().execute(
                sketch,
                ImageRequest(context, ""),
                false
            ).apply {
                assertTrue(this is ImageResult.Error)
            }

            RequestExecutor().execute(
                sketch,
                ImageRequest(context, "  "),
                false
            ).apply {
                assertTrue(this is ImageResult.Error)
            }
        }
    }

    @Test
    fun testGlobalImageOptions() = runTest {
        val (context, sketch) = getTestContextAndNewSketch {
        }
        val request = ImageRequest(context, ResourceImages.jpeg.uri).apply {
            assertEquals(Depth.NETWORK, depthHolder.depth)
            assertEquals(CachePolicy.ENABLED, downloadCachePolicy)
            assertEquals(SizeResolver(context.screenSize()), sizeResolver)
        }
        withContext(Dispatchers.Main) {
            RequestExecutor().execute(sketch, request, false).apply {
                assertEquals(Depth.NETWORK, this.request.depthHolder.depth)
                assertEquals(CachePolicy.ENABLED, this.request.downloadCachePolicy)
                assertEquals(SizeResolver(context.screenSize()), this.request.sizeResolver)
            }
        }

        val (context2, sketch2) = getTestContextAndNewSketch {
            globalImageOptions(ImageOptions {
                depth(MEMORY)
                downloadCachePolicy(WRITE_ONLY)
                resize(44, 67)
            })
        }
        val request2 = ImageRequest(context2, ResourceImages.jpeg.uri).apply {
            assertEquals(Depth.NETWORK, depthHolder.depth)
            assertEquals(CachePolicy.ENABLED, downloadCachePolicy)
            assertEquals(SizeResolver(context.screenSize()), sizeResolver)
        }
        withContext(Dispatchers.Main) {
            RequestExecutor().execute(sketch2, request2, false).apply {
                assertEquals(MEMORY, this.request.depthHolder.depth)
                assertEquals(WRITE_ONLY, this.request.downloadCachePolicy)
                assertTrue(this.request.sizeResolver is FixedSizeResolver)
            }
        }
    }
}