package org.bhashasetu.fln.edge.engine

import android.content.Context
import android.util.Log
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.InputStream

/**
 * Manages ONNX Runtime sessions for ASR, NMT, and TTS models.
 * Loads models from assets and provides inference sessions.
 *
 * Initialization is considered successful if at least ASR loads.
 * NMT/TTS failures are tracked individually so the UI can show
 * per-component status and fall back to phrasebook lookups.
 */
object OnnxEngineManager {
    private const val TAG = "OnnxEngine"
    private var env: OrtEnvironment? = null
    private var asrEncoderSession: OrtSession? = null
    private var asrDecoderSession: OrtSession? = null
    private var nmtEncoderSession: OrtSession? = null
    private var nmtDecoderSession: OrtSession? = null
    private var ttsSession: OrtSession? = null
    var tokenizer: IndicTransTokenizer? = null
        private set
    var isInitialized = false
        private set

    // Per-component status for UI reporting
    var asrStatus: String = "Not loaded"
        private set
    var nmtStatus: String = "Not loaded"
        private set
    var ttsStatus: String = "Not loaded"
        private set
    var initTimeMs: Long = 0
        private set

    fun initializeSessions(context: Context, targetLanguage: String = "sat") {
        if (isInitialized) return

        val initStart = System.nanoTime()
        Log.i(TAG, "════════════════════════════════════════")
        Log.i(TAG, "Initializing ONNX sessions (target=$targetLanguage)")
        Log.i(TAG, "════════════════════════════════════════")

        try {
            env = OrtEnvironment.getEnvironment()

            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(1)
                setInterOpNumThreads(1)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }

            // Load tokenizer
            try {
                tokenizer = IndicTransTokenizer(context, targetLanguage)
                Log.d(TAG, "Tokenizer loaded for $targetLanguage")
            } catch (e: Exception) {
                Log.w(TAG, "Tokenizer init failed (will use phrasebook): ${e.message}")
            }

            // ── ASR Model (Whisper Tiny) ──────────────────────────
            val asrStart = System.nanoTime()
            try {
                asrEncoderSession = env!!.createSession(
                    loadModelBytes(context, "models/asr/tiny-encoder.int8.onnx"),
                    sessionOptions
                )
                asrDecoderSession = env!!.createSession(
                    loadModelBytes(context, "models/asr/tiny-decoder.int8.onnx"),
                    sessionOptions
                )
                val asrMs = (System.nanoTime() - asrStart) / 1_000_000
                asrStatus = "Ready (${asrMs}ms)"
                Log.i(TAG, "✅ ASR sessions loaded in ${asrMs}ms")
            } catch (e: Exception) {
                asrStatus = "Failed: ${e.message}"
                Log.e(TAG, "❌ ASR model load failed: ${e.message}")
            }

            // ── NMT Model (encoder + decoder pair) ───────────────
            val nmtStart = System.nanoTime()
            val nmtPrefix = when (targetLanguage) {
                "ho" -> "models/nmt/encoder_ho_int8"
                "mnj" -> "models/nmt/encoder_mnj_int8"
                else -> "models/nmt/encoder_int8"
            }
            val nmtDecPrefix = when (targetLanguage) {
                "ho" -> "models/nmt/decoder_ho_int8"
                "mnj" -> "models/nmt/decoder_mnj_int8"
                else -> "models/nmt/decoder_int8"
            }

            try {
                nmtEncoderSession = env!!.createSession(
                    loadModelBytes(context, "$nmtPrefix.onnx"),
                    sessionOptions
                )
                nmtDecoderSession = env!!.createSession(
                    loadModelBytes(context, "$nmtDecPrefix.onnx"),
                    sessionOptions
                )
                val nmtMs = (System.nanoTime() - nmtStart) / 1_000_000
                nmtStatus = "Ready (${nmtMs}ms)"
                Log.i(TAG, "✅ NMT sessions loaded for $targetLanguage in ${nmtMs}ms")
            } catch (e: Exception) {
                nmtStatus = "Not available — using phrasebook fallback"
                Log.w(TAG, "⚠️  NMT model not found: ${e.message}")
                Log.w(TAG, "    Pipeline will use phrasebook lookup as fallback")
            }

            // ── TTS Model (VITS) ─────────────────────────────────
            val ttsStart = System.nanoTime()
            try {
                ttsSession = env!!.createSession(
                    loadModelBytes(context, "models/tts/sat_vits_int8.onnx"),
                    sessionOptions
                )
                val ttsMs = (System.nanoTime() - ttsStart) / 1_000_000
                ttsStatus = "Ready (${ttsMs}ms)"
                Log.i(TAG, "✅ TTS session loaded in ${ttsMs}ms")
            } catch (e: Exception) {
                ttsStatus = "Not available — text-only mode"
                Log.w(TAG, "⚠️  TTS model not found: ${e.message}")
                Log.w(TAG, "    Pipeline will output text only")
            }

            // ── Final status ─────────────────────────────────────
            // Consider initialized if at least ASR loaded (partial pipeline is usable)
            isInitialized = hasAsrModel()

            initTimeMs = (System.nanoTime() - initStart) / 1_000_000
            Log.i(TAG, "════════════════════════════════════════")
            Log.i(TAG, "Init complete in ${initTimeMs}ms")
            Log.i(TAG, "  ASR: $asrStatus")
            Log.i(TAG, "  NMT: $nmtStatus")
            Log.i(TAG, "  TTS: $ttsStatus")
            Log.i(TAG, "  Overall: ${if (isInitialized) "READY (partial pipeline OK)" else "FAILED"}")
            Log.i(TAG, "════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical initialization failure: ${e.message}", e)
            release()
        }
    }

    private fun loadModelBytes(context: Context, fileName: String): ByteArray {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val bytes = inputStream.readBytes()
            inputStream.close()
            val sizeMb = bytes.size / (1024.0 * 1024.0)
            Log.d(TAG, "  Loaded $fileName (${String.format("%.1f", sizeMb)} MB)")
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "  Failed to load model: $fileName - ${e.message}")
            throw e
        }
    }

    /** Returns a human-readable summary of all model statuses. */
    fun getStatusSummary(): String {
        return buildString {
            appendLine("ASR: $asrStatus")
            appendLine("NMT: $nmtStatus")
            appendLine("TTS: $ttsStatus")
        }.trim()
    }

    fun getAsrEncoderSession(): OrtSession = asrEncoderSession
        ?: throw IllegalStateException("ASR Encoder Session uninitialized")
    fun getAsrDecoderSession(): OrtSession = asrDecoderSession
        ?: throw IllegalStateException("ASR Decoder Session uninitialized")
    fun getNmtEncoderSession(): OrtSession = nmtEncoderSession
        ?: throw IllegalStateException("NMT Encoder Session uninitialized")
    fun getNmtDecoderSession(): OrtSession = nmtDecoderSession
        ?: throw IllegalStateException("NMT Decoder Session uninitialized")
    fun getTtsSession(): OrtSession = ttsSession
        ?: throw IllegalStateException("TTS Session uninitialized")

    fun hasAsrModel(): Boolean = asrEncoderSession != null && asrDecoderSession != null
    fun hasNmtModel(): Boolean = nmtEncoderSession != null && nmtDecoderSession != null
    fun hasTtsModel(): Boolean = ttsSession != null

    fun release() {
        Log.d(TAG, "Releasing all ONNX sessions")
        asrEncoderSession?.close()
        asrDecoderSession?.close()
        nmtEncoderSession?.close()
        nmtDecoderSession?.close()
        ttsSession?.close()
        env?.close()
        asrEncoderSession = null
        asrDecoderSession = null
        nmtEncoderSession = null
        nmtDecoderSession = null
        ttsSession = null
        env = null
        isInitialized = false
        asrStatus = "Released"
        nmtStatus = "Released"
        ttsStatus = "Released"
    }
}
