package com.github.panpf.sketch.sample.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scrolls the sample app's benchmark image grid (`ImageGridBenchmarkActivity`, in the app's
 * `src/benchmark`) and measures what each new row of images costs.
 *
 * The grid scrolls itself when it receives a broadcast, so the measured block sends that broadcast
 * and then waits out the run. Every run makes the same flings, so the measure block always covers
 * the same scrolling and two builds can be compared iteration against iteration.
 *
 * Besides the frame timings it reports:
 * - `onRememberedSumMs` and `onRememberedCount`: the time Compose spent in `Compose:onRemembered`,
 *   where each new `AsyncImageState` sets up its loading, and how often that section ran.
 * - `onForgottenSumMs` and `onForgottenCount`: the same for state that leaves the composition.
 * - the CPU times of [AppCpuMetric].
 *
 * Run with:
 * `./gradlew :samples:androidBenchmark:connectedBenchmarkAndroidTest`
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class ImageGridScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollImageGridCompilationModeFull() = scrollImageGrid(CompilationMode.Full())

    private fun scrollImageGrid(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
            TraceSectionMetric("Compose:onRemembered", TraceSectionMetric.Mode.Sum, label = "onRemembered"),
            TraceSectionMetric("Compose:onForgotten", TraceSectionMetric.Mode.Sum, label = "onForgotten"),
            AppCpuMetric(),
        ),
        iterations = ITERATIONS,
        // Not StartupMode.COLD: that mode kills the app between setupBlock and the measured
        // block, because it exists to measure the start itself.
        startupMode = null,
        compilationMode = compilationMode,
        setupBlock = {
            // A new process for every iteration, so each run starts with an empty memory cache.
            killProcess()
            startActivityAndWait(Intent().setClassName(packageName, ACTIVITY))
            // The first screen's images decode on background threads. Let them finish so the
            // measured scroll starts from a quiet app.
            Thread.sleep(SETTLE_MILLIS)
        },
    ) {
        device.executeShellCommand(
            "am broadcast -a $ACTION_SCROLL -p $packageName " +
                "--ei flings $FLINGS --ei slotMillis $SLOT_MILLIS --ef velocity $VELOCITY"
        )
        Thread.sleep(FLINGS * SLOT_MILLIS + TAIL_MILLIS)
    }

    private companion object {
        const val TARGET_PACKAGE = "com.github.panpf.sketch.sample.benchmark"
        const val ACTIVITY = "com.github.panpf.sketch.sample.benchmark.ImageGridBenchmarkActivity"

        // These match ImageGridBenchmarkActivity's ACTION_SCROLL and its extras.
        const val ACTION_SCROLL = "com.github.panpf.sketch.sample.benchmark.SCROLL"

        const val ITERATIONS = 10
        const val SETTLE_MILLIS = 1500L

        // A fling at 12,000 px/s runs for about 1.6 s and about 6,900 px on a Pixel 4a: some 26
        // rows of four images. Twelve of them, two seconds apart, make a 24 s run.
        const val FLINGS = 12
        const val SLOT_MILLIS = 2000L
        const val VELOCITY = 12000f

        // Covers the delay before the app receives the broadcast, and the last frames.
        const val TAIL_MILLIS = 500L
    }
}
