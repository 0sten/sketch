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

package com.github.panpf.sketch.core.common.test.transform.internal

import com.github.panpf.sketch.Image
import com.github.panpf.sketch.decode.internal.DecodeInterceptorChain
import com.github.panpf.sketch.decode.internal.EngineDecodeInterceptor
import com.github.panpf.sketch.images.ResourceImages
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.resize.Precision.LESS_PIXELS
import com.github.panpf.sketch.resize.Scale.CENTER_CROP
import com.github.panpf.sketch.size
import com.github.panpf.sketch.test.singleton.getTestContextAndSketch
import com.github.panpf.sketch.test.utils.corners
import com.github.panpf.sketch.test.utils.runBlock
import com.github.panpf.sketch.test.utils.toRequestContext
import com.github.panpf.sketch.transform.CircleCropTransformation
import com.github.panpf.sketch.transform.TransformResult
import com.github.panpf.sketch.transform.Transformation
import com.github.panpf.sketch.transform.createCircleCropTransformed
import com.github.panpf.sketch.transform.internal.TransformationDecodeInterceptor
import com.github.panpf.sketch.util.Size
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransformationDecodeInterceptorTest {

    @Test
    fun testIntercept() = runTest {
        val (context, sketch) = getTestContextAndSketch()
        val interceptors =
            listOf(EngineDecodeInterceptor())

        runBlock {
            val request = ImageRequest(context, ResourceImages.jpeg.uri) {
                size(3000, 3000)
                precision(LESS_PIXELS)
            }
            val chain = DecodeInterceptorChain(
                requestContext = request.toRequestContext(sketch),
                fetchResult = null,
                interceptors = interceptors,
                index = 0
            )
            TransformationDecodeInterceptor().intercept(chain)
        }.getOrThrow().apply {
            assertEquals(Size(1291, 1936), image.size)
            assertNotEquals(
                listOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT),
                image.corners()
            )
            assertNull(transformeds)
        }

        runBlock {
            val request = ImageRequest(context, ResourceImages.jpeg.uri) {
                size(3000, 3000)
                precision(LESS_PIXELS)
                transformations(CircleCropTransformation())
            }
            val chain = DecodeInterceptorChain(
                requestContext = request.toRequestContext(sketch),
                fetchResult = null,
                interceptors = interceptors,
                index = 0
            )
            TransformationDecodeInterceptor().intercept(chain)
        }.getOrThrow().apply {
            assertEquals(Size(1291, 1291), image.size)
            assertEquals(
                listOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT),
                image.corners()
            )
            assertEquals(listOf(createCircleCropTransformed(CENTER_CROP)), transformeds)
        }

        runBlock {
            val request = ImageRequest(context, ResourceImages.jpeg.uri) {
                size(3000, 3000)
                precision(LESS_PIXELS)
                transformations(object : Transformation {
                    override val key: String
                        get() = "TestTransformation"

                    override suspend fun transform(
                        requestContext: RequestContext,
                        input: Image
                    ): TransformResult = TransformResult(input, "TestTransformation")
                })
            }
            val chain = DecodeInterceptorChain(
                requestContext = request.toRequestContext(sketch),
                fetchResult = null,
                interceptors = interceptors,
                index = 0
            )
            TransformationDecodeInterceptor().intercept(chain)
        }.getOrThrow().apply {
            assertEquals(Size(1291, 1936), image.size)
            assertNotEquals(
                listOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT),
                image.corners()
            )
            assertNotNull(transformeds)
        }

        runBlock {
            val request = ImageRequest(context, ResourceImages.jpeg.uri) {
                size(3000, 3000)
                precision(LESS_PIXELS)
                transformations(object : Transformation {
                    override val key: String
                        get() = "TestTransformation"

                    override suspend fun transform(
                        requestContext: RequestContext,
                        input: Image
                    ): TransformResult? = null
                })
            }
            val chain = DecodeInterceptorChain(
                requestContext = request.toRequestContext(sketch),
                fetchResult = null,
                interceptors = interceptors,
                index = 0
            )
            TransformationDecodeInterceptor().intercept(chain)
        }.getOrThrow().apply {
            assertEquals(Size(1291, 1936), image.size)
            assertNotEquals(
                listOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT),
                image.corners()
            )
            assertNull(transformeds)
        }
    }

    @Test
    fun testSortWeight() {
        TransformationDecodeInterceptor().apply {
            assertEquals(90, sortWeight)
        }
    }

    @Test
    fun testEquals() {
        val ele1 = TransformationDecodeInterceptor()
        val ele2 = TransformationDecodeInterceptor()
        assertEquals(ele1, ele1)
        assertEquals(ele1, ele2)
        assertNotEquals(ele1, Any())
        assertNotEquals(ele1, null as Any?)
    }

    @Test
    fun testHashCode() {
        val ele1 = TransformationDecodeInterceptor()
        val ele2 = TransformationDecodeInterceptor()
        assertEquals(ele1.hashCode(), ele2.hashCode())
        assertNotEquals(ele1.hashCode(), Any().hashCode())
    }

    @Test
    fun testToString() {
        assertEquals(
            "TransformationDecodeInterceptor(sortWeight=90)",
            TransformationDecodeInterceptor().toString()
        )
    }
}