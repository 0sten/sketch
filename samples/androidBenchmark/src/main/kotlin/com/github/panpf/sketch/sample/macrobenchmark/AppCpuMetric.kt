package com.github.panpf.sketch.sample.macrobenchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor

/**
 * CPU time the app spent inside each iteration's measure block, in milliseconds.
 *
 * Frame timings say whether a frame was late. They do not say how much work the app did to make
 * it: a change that removes main-thread work leaves the timings flat while the device keeps up,
 * and frames that already finish early cannot get any earlier. CPU time shows that work directly.
 *
 * - `mainThreadCpuMs` covers composition, layout, drawing and the coroutines that run on the
 *   main thread.
 * - `renderThreadCpuMs` covers the render thread.
 * - `appCpuMs` covers every thread of the app, including the image decoders.
 *
 * Each value is the time the scheduler had a thread of the app on a CPU, cut to the measure
 * block. The default macrobenchmark trace records the scheduler, so nothing extra is needed.
 */
@OptIn(ExperimentalMetricApi::class)
class AppCpuMetric : TraceMetric() {

    override fun getMeasurements(
        captureInfo: Metric.CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Metric.Measurement> {
        val row = traceSession.query(cpuSql(captureInfo.targetPackageName)).first()
        return listOf(
            Metric.Measurement("mainThreadCpuMs", row.double("main")),
            Metric.Measurement("renderThreadCpuMs", row.double("render")),
            Metric.Measurement("appCpuMs", row.double("total")),
        )
    }

    private fun cpuSql(app: String): String {
        // The part of a scheduler slice that falls inside the measure block.
        val clipped = "MIN(sched.ts + sched.dur, win.e) - MAX(sched.ts, win.s)"
        return """
            WITH win AS (
              SELECT ts AS s, ts + dur AS e FROM slice WHERE name = 'measureBlock' ORDER BY ts LIMIT 1
            ),
            app_threads AS (
              SELECT utid, thread.name AS thread_name, thread.tid = process.pid AS is_main
              FROM thread JOIN process USING (upid)
              WHERE process.name = '$app'
            )
            SELECT
              IFNULL(SUM($clipped), 0) / 1e6 AS total,
              IFNULL(SUM(CASE WHEN is_main THEN $clipped END), 0) / 1e6 AS main,
              IFNULL(SUM(CASE WHEN thread_name = 'RenderThread' THEN $clipped END), 0) / 1e6 AS render
            FROM sched JOIN app_threads USING (utid), win
            WHERE sched.dur > 0 AND sched.ts < win.e AND sched.ts + sched.dur > win.s
        """.trimIndent()
    }
}
