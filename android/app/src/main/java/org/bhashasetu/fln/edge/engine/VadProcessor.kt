package org.bhashasetu.fln.edge.engine

import kotlin.math.sqrt

/**
 * Simple energy-based Voice Activity Detection.
 * Detects speech vs silence in audio chunks.
 */
class VadProcessor(
    private val sampleRate: Int = 16000,
    private val thresholdDb: Float = -40f,
    private val minSpeechFrames: Int = 3
) {
    private var speechFrameCount = 0
    private var isSpeaking = false

    /**
     * Check if audio chunk contains speech.
     * @param audioChunk PCM float audio samples normalized to [-1.0, 1.0]
     * @return true if speech is detected
     */
    fun isSpeechDetected(audioChunk: FloatArray): Boolean {
        val rms = calculateRMS(audioChunk)
        val db = if (rms > 0) (20 * kotlin.math.log10(rms)).toFloat() else -100f

        if (db > thresholdDb) {
            speechFrameCount++
            if (speechFrameCount >= minSpeechFrames) {
                isSpeaking = true
            }
        } else {
            if (isSpeaking && speechFrameCount > 0) {
                isSpeaking = false
                speechFrameCount = 0
                return true
            }
            speechFrameCount = 0
            isSpeaking = false
        }

        return isSpeaking
    }

    fun reset() {
        speechFrameCount = 0
        isSpeaking = false
    }

    private fun calculateRMS(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (sample in samples) {
            sum += sample * sample
        }
        return sqrt(sum / samples.size).toFloat()
    }
}
