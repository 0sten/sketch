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

package com.github.panpf.sketch.images

import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.http.HttpHeaders
import com.github.panpf.sketch.http.HttpStack
import com.github.panpf.sketch.request.Extras
import com.github.panpf.sketch.util.toUri
import okio.BufferedSource
import okio.buffer
import okio.use

// TODO Repeated with MyImagesHttpStack
class ResourcesHttpStack(
    private val context: PlatformContext,
) : HttpStack {

    override suspend fun getResponse(
        url: String,
        httpHeaders: HttpHeaders?,
        extras: Extras?
    ): HttpStack.Response {
        val uri = url.toUri()
        if (uri.authority != "resource") {
            return ErrorResponse(
                403,
                "Invalid resource authority: ${uri.authority}, expected 'resource'"
            )
        }

        val targetResourceName = uri.pathSegments.first()
        val image = ResourceImages.values.find {
            it.resourceName == targetResourceName
        }
        return if (image != null) {
            ResourcesResponse(context, image)
        } else {
            ErrorResponse(404, "Not found resource")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ResourcesHttpStack
        if (context != other.context) return false
        return true
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        return result
    }

    override fun toString(): String {
        return "ResourcesHttpStack"
    }

    class ErrorResponse(
        override val code: Int,
        override val message: String,
    ) : HttpStack.Response {

        override val contentLength: Long
            get() = 0
        override val contentType: String
            get() = ""

        override fun getHeaderField(name: String): String? {
            return null
        }

        override suspend fun content(): HttpStack.Content = throw Exception()
    }

    class ResourcesResponse(
        val context: PlatformContext,
        val imageFile: ResourceImageFile,
    ) : HttpStack.Response {

        override val code: Int
            get() = 200

        override val message: String?
            get() = null

        override val contentLength: Long by lazy {
            imageFile.toDataSource(context).openSource().buffer().use { it.length() }
        }

        override val contentType: String = imageFile.mimeType

        override fun getHeaderField(name: String): String? = when (name.lowercase()) {
            "content-length" -> contentLength.toString()
            "content-type" -> contentType
            else -> null
        }

        override suspend fun content(): HttpStack.Content {
            return imageFile.toDataSource(context).openSource().slow(20).content()
        }
    }
}

fun BufferedSource.length(): Long {
    var length = 0L
    val buffer = ByteArray(1024 * 8)
    while (true) {
        val readLength = this.read(buffer)
        if (readLength > 0) {
            length += readLength.toLong()
        } else {
            break
        }
    }
    return length
}