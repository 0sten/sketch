package com.github.panpf.sketch.sample.benchmark

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.images.ComposeResImageFiles
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * An endless grid of local images that scrolls itself, for the scroll benchmark in
 * `:samples:androidBenchmark`.
 *
 * The benchmark starts a scroll run with a broadcast instead of touch input. A run is a series of
 * flings down the grid. Each fling starts at the same velocity and uses the platform's fling
 * physics, and each gets a fixed time slot. So every run scrolls the same distance at the same
 * speeds and lasts the same time, and two builds can be compared frame for frame. It also avoids
 * UiAutomator's input injection, which waits for window animations and stalls for seconds on a
 * device where one is stuck.
 *
 * Every cell is a plain [AsyncImage], so the per-image cost measured is Sketch's own. The grid
 * cycles through 27 small local images. After the first screen they all come from the memory
 * cache, which leaves the work of composing, remembering and forgetting each image's state as the
 * main cost of a new row.
 */
class ImageGridBenchmarkActivity : ComponentActivity() {

    private val scrollRuns = Channel<ScrollRun>(Channel.CONFLATED)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scrollRuns.trySend(
                ScrollRun(
                    flings = intent.getIntExtra(EXTRA_FLINGS, 10),
                    slotMillis = intent.getIntExtra(EXTRA_SLOT_MILLIS, 2000),
                    velocity = intent.getFloatExtra(EXTRA_VELOCITY, 12000f),
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            /* context = */ this,
            /* receiver = */ receiver,
            /* filter = */ IntentFilter(ACTION_SCROLL),
            /* flags = */ ContextCompat.RECEIVER_EXPORTED,
        )
        setContent {
            ImageGrid(scrollRuns)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    companion object {
        /** Starts a scroll run. The extras below configure it. */
        const val ACTION_SCROLL = "com.github.panpf.sketch.sample.benchmark.SCROLL"

        /** How many flings the run makes. */
        const val EXTRA_FLINGS = "flings"

        /** The time each fling gets, in milliseconds. The next fling starts when it is over. */
        const val EXTRA_SLOT_MILLIS = "slotMillis"

        /** The initial velocity of each fling, in pixels per second. */
        const val EXTRA_VELOCITY = "velocity"
    }
}

private class ScrollRun(val flings: Int, val slotMillis: Int, val velocity: Float)

private const val COLUMNS = 4

// Far more cells than a run can reach.
private const val ITEM_COUNT = 100_000

private val IMAGE_URIS: List<String> = (
    ComposeResImageFiles.statics +
        ComposeResImageFiles.clockExifs +
        ComposeResImageFiles.clockHor +
        ComposeResImageFiles.clockVer +
        ComposeResImageFiles.numbers
    ).map { it.uri }

@Composable
private fun ImageGrid(scrollRuns: ReceiveChannel<ScrollRun>) {
    val gridState = rememberLazyGridState()
    val flingBehavior = ScrollableDefaults.flingBehavior()
    LaunchedEffect(gridState, flingBehavior) {
        for (run in scrollRuns) {
            repeat(run.flings) {
                val slotStart = TimeSource.Monotonic.markNow()
                gridState.scroll {
                    with(flingBehavior) { performFling(run.velocity) }
                }
                delay(run.slotMillis.milliseconds - slotStart.elapsedNow())
            }
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(count = ITEM_COUNT, contentType = { 0 }) { index ->
            AsyncImage(
                uri = IMAGE_URIS[index % IMAGE_URIS.size],
                contentDescription = null,
                modifier = Modifier.aspectRatio(1f),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
