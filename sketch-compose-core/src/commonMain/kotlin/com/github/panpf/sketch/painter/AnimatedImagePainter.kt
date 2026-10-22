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

package com.github.panpf.sketch.painter

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import com.github.panpf.sketch.AnimatedImage
import com.github.panpf.sketch.Bitmap
import com.github.panpf.sketch.util.Rect
import com.github.panpf.sketch.util.RememberedCounter
import com.github.panpf.sketch.util.asComposeImageBitmap
import com.github.panpf.sketch.util.copyPixelsFrom
import com.github.panpf.sketch.util.erase
import com.github.panpf.sketch.util.ioCoroutineDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.TimeSource
import kotlin.time.measureTime

/**
 * Painter for drawing [com.github.panpf.sketch.AnimatedImage]
 *
 * @see com.github.panpf.sketch.compose.core.nonandroid.test.painter.AnimatedImagePainterTest
 */
class AnimatedImagePainter constructor(
    private val animatedImage: AnimatedImage,
    private val srcOffset: IntOffset = IntOffset.Companion.Zero,
    private val srcSize: IntSize = IntSize(animatedImage.width, animatedImage.height),
    private val filterQuality: FilterQuality = DrawScope.Companion.DefaultFilterQuality,
) : Painter(), AnimatablePainter, RememberObserver {

    internal val rememberedCounter: RememberedCounter = RememberedCounter()
    internal var coroutineScope: CoroutineScope? = null

    private var alpha: Float = DefaultAlpha
    private var colorFilter: ColorFilter? = null
    private var invalidateTick by mutableIntStateOf(0)
    private var loadFirstFrameJob: Job? = null
    private var composeBitmap: ImageBitmap? = null
    private val paint = Paint()
    private var animatedPlayer = AnimatedPlayer(
        animatedImage = animatedImage,
        onFrame = {
            composeBitmap = it
            invalidateSelf()
        },
        onRepeatEnd = {
            stopAnimation()
        },
    )

    override val intrinsicSize: Size = srcSize.toSize()

    init {
        validateSize(srcOffset = srcOffset, srcSize = srcSize)
    }

    override fun DrawScope.onDraw() {
        invalidateTick // Invalidate the scope when invalidateTick changes.
        val composeBitmap = composeBitmap
        if (composeBitmap != null) {
            val dstSize = IntSize(
                this@onDraw.size.width.roundToInt(),
                this@onDraw.size.height.roundToInt()
            )
            drawIntoCanvas {
                paint.colorFilter = colorFilter
                paint.alpha = alpha
                paint.filterQuality = filterQuality
                it.drawImageRect(
                    image = composeBitmap,
                    srcOffset = srcOffset,
                    srcSize = srcSize,
                    dstOffset = IntOffset.Companion.Zero,
                    dstSize = dstSize,
                    paint = paint,
                )

                val canvas: Canvas = it
                animatedImage.animatedTransformation
                    ?.invoke(canvas, Rect(0, 0, dstSize.width, dstSize.height))
            }
        } else if (!animatedPlayer.running && loadFirstFrameJob == null) {
            loadFirstFrame()
        }
    }

    private fun loadFirstFrame() {
        if (loadFirstFrameJob?.isActive == true) return
        @Suppress("OPT_IN_USAGE")
        loadFirstFrameJob = GlobalScope.launch(Dispatchers.Main) {
            val bitmapResult = withContext(ioCoroutineDispatcher()) {
                runCatching {
                    animatedImage.createFrameBitmap().apply {
                        animatedImage.readFrame(this@apply, 0)
                    }
                }
            }
            bitmapResult.exceptionOrNull()?.printStackTrace()
            composeBitmap = bitmapResult.getOrNull()?.asComposeImageBitmap()
            invalidateSelf()
        }
    }

    private fun startAnimation() {
        coroutineScope ?: return
        if (animatedPlayer.running) return
        if (loadFirstFrameJob?.isActive == true) {
            loadFirstFrameJob?.cancel("startAnimation")
        }
        animatedPlayer.start(coroutineScope!!)
        animatedImage.animationStartCallback?.invoke()
    }

    private fun stopAnimation() {
        coroutineScope ?: return
        if (!animatedPlayer.running) return
        animatedPlayer.stop()
        animatedImage.animationEndCallback?.invoke()
    }

    /**
     * Note: Do not actively call its onRemembered method because this will destroy the remembered count.
     */
    override fun onRemembered() {
        if (!rememberedCounter.remember()) return
        coroutineScope = CoroutineScope(Dispatchers.Main)
        startAnimation()
    }

    override fun onAbandoned() = onForgotten()
    override fun onForgotten() {
        if (!rememberedCounter.forget()) return
        stopAnimation()
        // The scope is not cancelled: once stopped, the animation returns by itself. Cancelling
        // it would throw a CancellationException through its suspended frames instead, and on
        // Kotlin/Native every throw unwinds the stack with libunwind.
        coroutineScope = null
    }

    private fun invalidateSelf() {
        if (invalidateTick == Int.MAX_VALUE) {
            invalidateTick = 0
        } else {
            invalidateTick++
        }
    }

    private fun validateSize(srcOffset: IntOffset, srcSize: IntSize): IntSize {
        require(
            srcOffset.x >= 0 &&
                    srcOffset.y >= 0 &&
                    srcSize.width >= 0 &&
                    srcSize.height >= 0 &&
                    srcSize.width <= animatedImage.width &&
                    srcSize.height <= animatedImage.height
        )
        return srcSize
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun start() {
        if (rememberedCounter.isRemembered) {
            startAnimation()
        }
    }

    override fun stop() {
        if (rememberedCounter.isRemembered) {
            stopAnimation()
        }
    }

    override fun isRunning(): Boolean = animatedPlayer.running

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as AnimatedImagePainter
        if (animatedImage != other.animatedImage) return false
        if (srcOffset != other.srcOffset) return false
        if (srcSize != other.srcSize) return false
        if (filterQuality != other.filterQuality) return false
        return true
    }

    override fun hashCode(): Int {
        var result = animatedImage.hashCode()
        // Because srcOffset and srcSize are value classes, they will be replaced by long.
        // Long will lose precision when converting hashCode, causing the hashCode generated by different srcOffset and srcSize to be the same.
        result = 31 * result + srcOffset.toString().hashCode()
        result = 31 * result + srcSize.toString().hashCode()
        result = 31 * result + filterQuality.hashCode()
        return result
    }

    override fun toString(): String {
        return "AnimatedImagePainter(" +
                "animatedImage=$animatedImage, " +
                "srcOffset=$srcOffset, " +
                "srcSize=$srcSize, " +
                "filterQuality=$filterQuality)"
    }

    /**
     * Plays [animatedImage] in one coroutine: it shows a frame, decodes the next one on the IO
     * dispatcher, and waits for the rest of the frame's duration. [stop] completes the run's
     * stop signal, which the coroutine checks after decoding and waits on with the frame's
     * duration as a timeout, so it returns without being cancelled.
     */
    private class AnimatedPlayer(
        private val animatedImage: AnimatedImage,
        private val onFrame: (ImageBitmap) -> Unit,
        private val onRepeatEnd: () -> Unit,
    ) {

        private var stopSignal: CompletableDeferred<Unit>? = null
        private var currentFrame: Frame? = null

        var running: Boolean = false
            private set

        fun start(coroutineScope: CoroutineScope) {
            if (running) {
                return
            }
            if (animatedImage.frameCount <= 0) {
                val blackBitmap = animatedImage.createFrameBitmap().apply {
                    erase(Color.Black.toArgb())
                }
                onFrame(blackBitmap.asComposeImageBitmap())
                return
            }

            running = true
            val stopSignal = CompletableDeferred<Unit>()
            this.stopSignal = stopSignal
            coroutineScope.launch(Dispatchers.Main) {
                play(stopSignal)
            }
        }

        fun stop() {
            if (!running) {
                return
            }
            running = false
            stopSignal?.complete(Unit)
            stopSignal = null
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private suspend fun play(stopSignal: CompletableDeferred<Unit>) {
            if (stopSignal.isCompleted) return
            // Frames whose decoding took longer than the previous frame's duration, see decode()
            val frameCaches = mutableMapOf<Int, Bitmap>()
            var repeatIndex = 0
            var frame = nextFrame(current = currentFrame, last = null)
            if (!decode(frame, frameCaches)) {
                stopAfterDecodeError(stopSignal)
                return
            }
            while (!stopSignal.isCompleted) {
                val lastFrame = currentFrame
                currentFrame = frame
                onFrame(frame.frameBitmap.composeBitmap)

                // Number of repeat plays. -1: Indicates infinite repetition. When it is greater than or equal to 0, the total number of plays is equal to '1 + repeatCount'
                if (animatedImage.repeatCount >= 0 && frame.index == animatedImage.frameCount - 1) {
                    repeatIndex++
                }
                if (animatedImage.repeatCount >= 0 && repeatIndex > animatedImage.repeatCount) {
                    onRepeatEnd()
                    return
                }

                // The next frame is decoded into the bitmap of the frame shown before this one,
                // which is no longer drawn, while this one is shown.
                val shownMark = TimeSource.Monotonic.markNow()
                val nextFrame = nextFrame(current = frame, last = lastFrame)
                if (!decode(nextFrame, frameCaches)) {
                    stopAfterDecodeError(stopSignal)
                    return
                }
                val remainingMillis =
                    frameDuration(frame.index) - shownMark.elapsedNow().inWholeMilliseconds
                if (stopSignal.isCompleted) return
                if (remainingMillis > 0) {
                    val stopped = select {
                        stopSignal.onAwait { true }
                        onTimeout(remainingMillis) { false }
                    }
                    if (stopped) return
                }
                frame = nextFrame
            }
        }

        /**
         * Returns false if the frame could not be decoded
         */
        private suspend fun decode(
            frame: Frame,
            frameCaches: MutableMap<Int, Bitmap>
        ): Boolean = withContext(ioCoroutineDispatcher()) {
            frame.frameBitmap.bitmap.erase(Color.Transparent.toArgb())

            val cacheBitmap = frameCaches[frame.index]
            if (cacheBitmap != null) {
                frame.frameBitmap.bitmap.copyPixelsFrom(cacheBitmap)
                return@withContext true
            }

            var decoded = true
            val decodeElapsedTime = measureTime {
                try {
                    animatedImage.readFrame(frame.frameBitmap.bitmap, frame.index)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    decoded = false
                }
            }

            /*
             * The closer Codec is to the last frame when decoding animations,
             * the longer the decoding time will be.
             * This will cause the animation playback speed to become slower and slower,
             * so here the frames whose decoding time exceeds the duration of the previous frame are cached.
             */
            if (decoded && animatedImage.cacheDecodeTimeoutFrame) {
                val lastFrameIndex = if (frame.index > 0)
                    frame.index - 1 else animatedImage.frameCount - 1
                val lastFrameDuration = frameDuration(lastFrameIndex)
                val needCache = decodeElapsedTime.inWholeMilliseconds > lastFrameDuration
                if (needCache) {
                    val bitmap = animatedImage.createFrameBitmap()
                    bitmap.copyPixelsFrom(frame.frameBitmap.bitmap)
                    frameCaches[frame.index] = bitmap
                }
            }
            decoded
        }

        private fun stopAfterDecodeError(stopSignal: CompletableDeferred<Unit>) {
            // Only if this run is still the current one, it may have been stopped and restarted
            if (this.stopSignal === stopSignal) {
                stop()
            }
        }

        private fun nextFrame(current: Frame?, last: Frame?): Frame {
            val nextFrameIndex = if (current != null) {
                (current.index + 1) % animatedImage.frameCount
            } else {
                0
            }
            return last?.copy(index = nextFrameIndex)
                ?: Frame(
                    index = nextFrameIndex,
                    frameBitmap = FrameBitmap(animatedImage.createFrameBitmap())
                )
        }

        private fun frameDuration(index: Int): Long {
            return animatedImage.frameDurations
                .getOrNull(index)?.toLong()
                ?.takeIf { it > 0L }
                ?: 100L
        }

        private class FrameBitmap(val bitmap: Bitmap) {
            val composeBitmap: ImageBitmap = bitmap.asComposeImageBitmap()
        }

        private data class Frame(val index: Int, val frameBitmap: FrameBitmap)
    }
}