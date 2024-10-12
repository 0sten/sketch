package com.github.panpf.sketch.compose.core.common.test.transition

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import com.github.panpf.sketch.asImage
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.images.ResourceImages
import com.github.panpf.sketch.painter.ComposeBitmapPainter
import com.github.panpf.sketch.painter.CrossfadePainter
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.ImageResult
import com.github.panpf.sketch.resize.Precision
import com.github.panpf.sketch.resize.Resize
import com.github.panpf.sketch.resize.Scale
import com.github.panpf.sketch.source.DataFrom.LOCAL
import com.github.panpf.sketch.source.DataFrom.MEMORY_CACHE
import com.github.panpf.sketch.target.TestGenericComposeTarget
import com.github.panpf.sketch.test.singleton.getTestContextAndSketch
import com.github.panpf.sketch.test.utils.createBitmap
import com.github.panpf.sketch.test.utils.toRequestContext
import com.github.panpf.sketch.transition.ComposeCrossfadeTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposeCrossfadeTransitionTest {

    @Test
    fun testConstructor() = runTest {
        val (context, sketch) = getTestContextAndSketch()
        val imageViewTarget = TestGenericComposeTarget()
        val request = ImageRequest(context, ResourceImages.jpeg.uri)
        val requestContext = request.toRequestContext(sketch)
        val resultBitmap = createBitmap(100, 200)
        val result = ImageResult.Success(
            request = request,
            cacheKey = requestContext.cacheKey,
            image = resultBitmap.asImage(),
            imageInfo = ImageInfo(100, 200, "image/jpeg"),
            dataFrom = LOCAL,
            resize = Resize(100, 100, Precision.LESS_PIXELS, Scale.CENTER_CROP),
            transformeds = null,
            extras = null,
        )
        ComposeCrossfadeTransition(sketch, request, imageViewTarget, result).apply {
            assertEquals(200, durationMillis)
            assertEquals(false, preferExactIntrinsicSize)
            assertEquals(true, fitScale)
        }
        ComposeCrossfadeTransition(
            sketch = sketch,
            request = request,
            target = imageViewTarget,
            result = result,
            durationMillis = 300,
            preferExactIntrinsicSize = true,
            fitScale = false
        ).apply {
            assertEquals(300, durationMillis)
            assertEquals(true, preferExactIntrinsicSize)
            assertEquals(false, fitScale)
        }
        assertFailsWith(IllegalArgumentException::class) {
            ComposeCrossfadeTransition(sketch, request, imageViewTarget, result, durationMillis = 0)
        }
    }

    @Test
    fun testTransition() = runTest {
        val (context, sketch) = getTestContextAndSketch()
        val request = ImageRequest(context, ResourceImages.jpeg.uri)

        val imageViewTarget = TestGenericComposeTarget()

        assertNull(imageViewTarget.painter)
        assertEquals(false, imageViewTarget.isStarted)
        imageViewTarget.isStarted = true
        assertEquals(true, imageViewTarget.isStarted)

        // success
        withContext(Dispatchers.Main) {
            imageViewTarget.onSuccess(sketch, request, ColorPainter(Color.Green).asImage())
        }
        assertEquals(Color.Green, (imageViewTarget.painter as ColorPainter).color)
        val resultBitmap = createBitmap(100, 200)
        val success = ImageResult.Success(
            request = request,
            cacheKey = request.toRequestContext(sketch).cacheKey,
            image = resultBitmap.asImage(),
            imageInfo = ImageInfo(100, 200, "image/jpeg"),
            resize = Resize(100, 100, Precision.LESS_PIXELS, Scale.CENTER_CROP),
            dataFrom = LOCAL,
            transformeds = null,
            extras = null,
        )
        ComposeCrossfadeTransition(sketch, request, imageViewTarget, success).transition()
        (imageViewTarget.painter as CrossfadePainter).apply {
            assertEquals(Color.Green, (start as ColorPainter).color)
            assertTrue(end is ComposeBitmapPainter, message = "end is $end")
            assertTrue(fitScale)
        }

        // error
        withContext(Dispatchers.Main) {
            imageViewTarget.onSuccess(sketch, request, ColorPainter(Color.Green).asImage())
        }
        assertEquals(Color.Green, (imageViewTarget.painter as ColorPainter).color)
        val error = ImageResult.Error(
            request = request,
            image = resultBitmap.asImage(),
            throwable = Exception(""),
        )
        ComposeCrossfadeTransition(sketch, request, imageViewTarget, error).transition()
        (imageViewTarget.painter as CrossfadePainter).apply {
            assertEquals(Color.Green, (start as ColorPainter).color)
        }

        // start end same
        withContext(Dispatchers.Main) {
            imageViewTarget.onSuccess(sketch, request, ColorPainter(Color.Green).asImage())
        }
        assertTrue(imageViewTarget.painter!! is ColorPainter)
        ComposeCrossfadeTransition(
            sketch = sketch,
            request = request,
            target = imageViewTarget,
            result = ImageResult.Success(
                request = request,
                cacheKey = request.toRequestContext(sketch).cacheKey,
                image = imageViewTarget.painter!!.asImage(),
                imageInfo = ImageInfo(100, 200, "image/jpeg"),
                dataFrom = LOCAL,
                resize = Resize(100, 100, Precision.LESS_PIXELS, Scale.CENTER_CROP),
                transformeds = null,
                extras = null,
            )
        ).transition()
        assertTrue(imageViewTarget.painter!! is ColorPainter)
    }

    @Test
    fun testFactoryConstructor() {
        ComposeCrossfadeTransition.Factory().apply {
            assertEquals(200, durationMillis)
            assertEquals(false, preferExactIntrinsicSize)
            assertEquals(false, alwaysUse)
        }

        assertFailsWith(IllegalArgumentException::class) {
            ComposeCrossfadeTransition.Factory(0)
        }

        ComposeCrossfadeTransition.Factory(
            durationMillis = 300,
            preferExactIntrinsicSize = true,
            alwaysUse = true
        ).apply {
            assertEquals(300, durationMillis)
            assertEquals(true, preferExactIntrinsicSize)
            assertEquals(true, alwaysUse)
        }
    }

    @Test
    fun testFactoryCreate() = runTest {
        val (context, sketch) = getTestContextAndSketch()
        val request = ImageRequest(context, ResourceImages.jpeg.uri)
        val factory = ComposeCrossfadeTransition.Factory()

        val imageViewTarget = TestGenericComposeTarget()

        val resultBitmap = createBitmap(100, 200)

        val successResult = ImageResult.Success(
            request = request,
            cacheKey = request.toRequestContext(sketch).cacheKey,
            image = resultBitmap.asImage(),
            imageInfo = ImageInfo(100, 200, "image/jpeg"),
            dataFrom = LOCAL,
            resize = Resize(100, 100, Precision.LESS_PIXELS, Scale.CENTER_CROP),
            transformeds = null,
            extras = null,
        )
        assertNotNull(factory.create(sketch, request, imageViewTarget, successResult))

        val errorResult = ImageResult.Error(
            request = request,
            image = resultBitmap.asImage(),
            throwable = Exception("")
        )
        assertNotNull(factory.create(sketch, request, imageViewTarget, errorResult))

        val fromMemoryCacheSuccessResult = ImageResult.Success(
            request = request,
            cacheKey = request.toRequestContext(sketch).cacheKey,
            image = resultBitmap.asImage(),
            imageInfo = ImageInfo(100, 200, "image/jpeg"),
            dataFrom = MEMORY_CACHE,
            resize = Resize(100, 100, Precision.LESS_PIXELS, Scale.CENTER_CROP),
            transformeds = null,
            extras = null,
        )
        assertNull(
            factory.create(
                sketch = sketch,
                request = request,
                target = imageViewTarget,
                result = fromMemoryCacheSuccessResult
            )
        )

        val alwaysUseFactory = ComposeCrossfadeTransition.Factory(alwaysUse = true)
        assertNotNull(
            alwaysUseFactory.create(sketch, request, imageViewTarget, fromMemoryCacheSuccessResult)
        )
    }

    @Test
    fun testFactoryEqualsAndHashCode() {
        val element1 = ComposeCrossfadeTransition.Factory()
        val element11 = ComposeCrossfadeTransition.Factory()
        val element2 = ComposeCrossfadeTransition.Factory(durationMillis = 300)
        val element3 = ComposeCrossfadeTransition.Factory(preferExactIntrinsicSize = true)
        val element4 = ComposeCrossfadeTransition.Factory(alwaysUse = true)
        val element5 = ComposeCrossfadeTransition.Factory(fadeStart = false)

        assertEquals(element1, element11)
        assertNotEquals(element1, element2)
        assertNotEquals(element1, element3)
        assertNotEquals(element1, element4)
        assertNotEquals(element1, element5)
        assertNotEquals(element2, element3)
        assertNotEquals(element2, element4)
        assertNotEquals(element3, element4)
        assertNotEquals(element1, null as Any?)
        assertNotEquals(element1, Any())

        assertEquals(element1.hashCode(), element11.hashCode())
        assertNotEquals(element1.hashCode(), element2.hashCode())
        assertNotEquals(element1.hashCode(), element3.hashCode())
        assertNotEquals(element1.hashCode(), element4.hashCode())
        assertNotEquals(element1.hashCode(), element5.hashCode())
        assertNotEquals(element2.hashCode(), element3.hashCode())
        assertNotEquals(element2.hashCode(), element4.hashCode())
        assertNotEquals(element3.hashCode(), element4.hashCode())
    }

    @Test
    fun testFactoryKey() {
        val element1 = ComposeCrossfadeTransition.Factory()
        val element2 = ComposeCrossfadeTransition.Factory(preferExactIntrinsicSize = true)
        val element3 = ComposeCrossfadeTransition.Factory(alwaysUse = true)
        val element4 = ComposeCrossfadeTransition.Factory(fadeStart = false)

        assertEquals(
            "ComposeCrossfade(200,true,false,false)",
            element1.key
        )
        assertEquals(
            "ComposeCrossfade(200,true,true,false)",
            element2.key
        )
        assertEquals(
            "ComposeCrossfade(200,true,false,true)",
            element3.key
        )
        assertEquals(
            "ComposeCrossfade(200,false,false,false)",
            element4.key
        )
    }

    @Test
    fun testFactoryToString() {
        val element1 = ComposeCrossfadeTransition.Factory()
        val element2 = ComposeCrossfadeTransition.Factory(preferExactIntrinsicSize = true)
        val element3 = ComposeCrossfadeTransition.Factory(alwaysUse = true)
        val element4 = ComposeCrossfadeTransition.Factory(fadeStart = false)

        assertEquals(
            "ComposeCrossfadeTransition.Factory(durationMillis=200, fadeStart=true, preferExactIntrinsicSize=false, alwaysUse=false)",
            element1.toString()
        )
        assertEquals(
            "ComposeCrossfadeTransition.Factory(durationMillis=200, fadeStart=true, preferExactIntrinsicSize=true, alwaysUse=false)",
            element2.toString()
        )
        assertEquals(
            "ComposeCrossfadeTransition.Factory(durationMillis=200, fadeStart=true, preferExactIntrinsicSize=false, alwaysUse=true)",
            element3.toString()
        )
        assertEquals(
            "ComposeCrossfadeTransition.Factory(durationMillis=200, fadeStart=false, preferExactIntrinsicSize=false, alwaysUse=false)",
            element4.toString()
        )
    }
}