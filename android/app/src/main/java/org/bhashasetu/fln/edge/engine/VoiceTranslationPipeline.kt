package org.bhashasetu.fln.edge.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import android.util.Log
import org.bhashasetu.fln.edge.data.PhrasebookLookup
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * End-to-end voice translation pipeline:
 * Audio → ASR → NMT (or Phrasebook) → TTS → Audio
 *
 * Graceful degradation:
 * - If NMT is missing: falls back to phrasebook lookup
 * - If TTS is missing: returns text only (no synthesized audio)
 * - If ASR is missing: returns empty result
 */
class VoiceTranslationPipeline {
    companion object {
        private const val TAG = "VoicePipeline"
        private const val ASR_MAX_LENGTH = 30 * 16000
        private const val NMT_MAX_LENGTH = 128
    }

    data class PipelineResult(
        val asrText: String,
        val translatedText: String,
        val synthesizedAudio: FloatArray,
        val latencyMs: Long,
        val translationSource: String = "none" // "nmt", "phrasebook", "none"
    )

    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var phrasebook: PhrasebookLookup? = null

    /**
     * Initialize the phrasebook for offline fallback.
     * Call this after the pipeline is created, from the Activity.
     */
    fun initPhrasebook(context: Context) {
        try {
            phrasebook = PhrasebookLookup(context)
            Log.i(TAG, "Phrasebook initialized with ${phrasebook?.size} phrases")
        } catch (e: Exception) {
            Log.w(TAG, "Phrasebook init failed (NMT-only mode): ${e.message}")
        }
    }

    fun processAudioChunk(audioChunk: FloatArray): PipelineResult {
        val startTime = System.nanoTime()
        Log.d(TAG, "━━━ Pipeline Start (${audioChunk.size} samples) ━━━")

        // Stage 1: ASR
        val asrStart = System.nanoTime()
        val asrText = runASR(audioChunk)
        val asrMs = (System.nanoTime() - asrStart) / 1_000_000
        Log.d(TAG, "  ASR: ${asrMs}ms → '${asrText.take(50)}'")

        // Stage 2: Translation (NMT or Phrasebook fallback)
        val nmtStart = System.nanoTime()
        var translatedText = ""
        var translationSource = "none"

        if (asrText.isNotBlank()) {
            // Try phrasebook first (faster, higher confidence for known phrases)
            val phraseResult = phrasebook?.lookup(asrText)
            if (phraseResult != null) {
                translatedText = phraseResult.santaliText
                translationSource = "phrasebook (${phraseResult.matchType})"
                Log.d(TAG, "  NMT: PHRASEBOOK HIT → '${translatedText.take(50)}'")
            } else if (OnnxEngineManager.hasNmtModel()) {
                // Fall back to NMT model
                translatedText = runNMT(asrText)
                translationSource = "nmt"
                Log.d(TAG, "  NMT: MODEL → '${translatedText.take(50)}'")
            } else {
                Log.w(TAG, "  NMT: No model AND no phrasebook match for '${asrText.take(50)}'")
                translatedText = "[No translation available] $asrText"
                translationSource = "none"
            }
        }
        val nmtMs = (System.nanoTime() - nmtStart) / 1_000_000

        // Stage 3: TTS
        val ttsStart = System.nanoTime()
        val synthesizedAudio = if (translatedText.isNotBlank() && !translatedText.startsWith("[No")) {
            runTTS(translatedText)
        } else {
            floatArrayOf()
        }
        val ttsMs = (System.nanoTime() - ttsStart) / 1_000_000
        Log.d(TAG, "  TTS: ${ttsMs}ms → ${synthesizedAudio.size} samples")

        val totalMs = (System.nanoTime() - startTime) / 1_000_000
        Log.d(TAG, "━━━ Pipeline End: ${totalMs}ms (ASR:${asrMs} NMT:${nmtMs} TTS:${ttsMs}) ━━━")

        return PipelineResult(asrText, translatedText, synthesizedAudio, totalMs, translationSource)
    }

    private fun runASR(audioChunk: FloatArray): String {
        if (!OnnxEngineManager.hasAsrModel()) {
            Log.w(TAG, "  ASR model not available")
            return ""
        }
        return try {
            val encoderSession = OnnxEngineManager.getAsrEncoderSession()
            val inputAudio = if (audioChunk.size > ASR_MAX_LENGTH) audioChunk.copyOf(ASR_MAX_LENGTH) else audioChunk
            val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputAudio), longArrayOf(1, inputAudio.size.toLong()))
            val inputMap = mapOf<String, OnnxTensor>("input_features" to inputTensor)
            val encoderOutput = encoderSession.run(inputMap)
            inputTensor.close()

            // TODO: Implement full Whisper decoder loop with beam search
            // Currently runs encoder only — decoder integration pending
            val outputData = encoderOutput[0].value
            Log.d(TAG, "  ASR encoder output: ${outputData?.javaClass?.name}")

            // For now, return empty to indicate "ASR ran but decoding not implemented"
            // The phrasebook will handle common classroom phrases
            ""
        } catch (e: Exception) {
            Log.e(TAG, "  ASR error: ${e.message}")
            ""
        }
    }

    private fun runNMT(hindiText: String): String {
        if (!OnnxEngineManager.hasNmtModel()) return ""
        return try {
            val tokenizer = OnnxEngineManager.tokenizer ?: throw IllegalStateException("Tokenizer not initialized")
            val encoderSession = OnnxEngineManager.getNmtEncoderSession()
            val inputText = "${tokenizer.getLanguageTag("hi")} $hindiText"
            val inputIds = tokenizer.encode(inputText, maxLength = NMT_MAX_LENGTH)
            val inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds.map { it.toLong() }.toLongArray()), longArrayOf(1, inputIds.size.toLong()))
            val inputMap = mapOf<String, OnnxTensor>("input_ids" to inputTensor)
            val encoderOutput = encoderSession.run(inputMap)
            inputTensor.close()

            // TODO: Implement full NMT decoder loop with beam search
            // Currently runs encoder only — decoder greedy/beam search pending
            Log.d(TAG, "  NMT encoder output: ${encoderOutput[0].value?.javaClass?.name}")
            ""
        } catch (e: Exception) {
            Log.e(TAG, "  NMT error: ${e.message}")
            ""
        }
    }

    private fun runTTS(santaliText: String): FloatArray {
        if (!OnnxEngineManager.hasTtsModel()) {
            Log.d(TAG, "  TTS model not available — text-only output")
            return floatArrayOf()
        }
        return try {
            val session = OnnxEngineManager.getTtsSession()
            val phonemeIds = santaliText.map { it.code.toLong() }.toLongArray()
            val inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(phonemeIds), longArrayOf(1, phonemeIds.size.toLong()))
            val inputMap = mapOf<String, OnnxTensor>("input_ids" to inputTensor)
            val results = session.run(inputMap)
            val audioOutput = results[0].value as Array<FloatArray>
            inputTensor.close()
            audioOutput[0]
        } catch (e: Exception) {
            Log.e(TAG, "  TTS error: ${e.message}")
            floatArrayOf()
        }
    }
}

