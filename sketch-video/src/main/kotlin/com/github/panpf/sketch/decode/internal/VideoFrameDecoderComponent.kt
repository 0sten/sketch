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

package com.github.panpf.sketch.decode.internal

import androidx.annotation.Keep
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.VideoFrameDecoder
import com.github.panpf.sketch.util.ComponentDetector
import com.github.panpf.sketch.util.DecoderComponent

/**
 * Cooperate with [ComponentDetector] to achieve automatic registration [VideoFrameDecoder]
 *
 * @see com.github.panpf.sketch.video.test.decode.internal.VideoFrameDecoderComponentTest
 */
@Keep
class VideoFrameDecoderComponent : DecoderComponent {

    override fun factory(context: PlatformContext): Decoder.Factory {
        return VideoFrameDecoder.Factory()
    }
}