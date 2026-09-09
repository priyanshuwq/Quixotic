package org.bhashasetu.fln.edge.engine

import android.os.Debug
import android.util.Log

object PerformanceBenchmark {
    private const val TAG = "PerfBenchmark"

    data class BenchmarkResult(
        val avgLatencyMs: Long,
        val maxLatencyMs: Long,
        val avgMemoryMB: Float,
        val passed: Boolean
    )

    fun runBenchmark(pipeline: VoiceTranslationPipeline, testChunks: List<FloatArray>, iterations: Int = 100): BenchmarkResult {
        val latencies = mutableListOf<Long>()
        val memBefore = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        for (i in 0 until iterations) {
            val start = System.nanoTime()
            pipeline.processAudioChunk(testChunks[i % testChunks.size])
            latencies.add((System.nanoTime() - start) / 1_000_000)
        }

        val memAfter = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        val avgLatency = latencies.average().toLong()
        val maxLatency = latencies.maxOrNull() ?: 0L
        val avgMemory = (memAfter - memBefore).toFloat()

        val passed = avgLatency <= 600 && avgMemory <= 450
        Log.d(TAG, "Latency: avg=${avgLatency}ms max=${maxLatency}ms | Memory: ${avgMemory}MB | Pass=$passed")

        return BenchmarkResult(avgLatency, maxLatency, avgMemory, passed)
    }
}
