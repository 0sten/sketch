package com.github.panpf.sketch.test.utils

import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.request.RequestInterceptor
import com.github.panpf.sketch.request.RequestInterceptor.Chain
import com.github.panpf.sketch.resize.Precision
import com.github.panpf.sketch.resize.Resize
import com.github.panpf.sketch.resize.Scale
import com.github.panpf.sketch.source.DataFrom

class TestMemoryCacheRequestIntercept : RequestInterceptor {

    override val key: String = "endRequestInterceptor"
    override val sortWeight: Int = 100

    var executeCount = 0

    override suspend fun intercept(chain: Chain): Result<ImageData> {
        executeCount++
        return Result.success(
            ImageData(
                image = FakeImage(100, 100),
                imageInfo = ImageInfo(100, 100, "image/png"),
                resize = Resize(100, 100, Precision.LESS_PIXELS, Scale.CENTER_CROP),
                dataFrom = DataFrom.LOCAL,
                transformeds = null,
                extras = null
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other != null && this::class == other::class
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }

    override fun toString(): String = "TestMemoryCacheRequestIntercept(sortWeight=$sortWeight)"
}