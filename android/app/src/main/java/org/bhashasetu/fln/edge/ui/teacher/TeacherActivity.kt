package org.bhashasetu.fln.edge.ui.teacher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bhashasetu.fln.edge.R
import org.bhashasetu.fln.edge.audio.AudioRecorder
import org.bhashasetu.fln.edge.data.CurriculumSeeder
import org.bhashasetu.fln.edge.engine.OnnxEngineManager
import org.bhashasetu.fln.edge.engine.VadProcessor
import org.bhashasetu.fln.edge.engine.VoiceTranslationPipeline

class TeacherActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "TeacherActivity"
    }

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var pipeline: VoiceTranslationPipeline
    private lateinit var vad: VadProcessor
    private var isTranslating = false

    private lateinit var tvStatus: TextView
    private lateinit var tvHindiText: TextView
    private lateinit var tvSantaliText: TextView
    private lateinit var tvLatency: TextView
    private lateinit var tvModelStatus: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher)

        // Initialize views
        tvStatus = findViewById(R.id.tvStatus)
        tvHindiText = findViewById(R.id.tvHindiText)
        tvSantaliText = findViewById(R.id.tvSantaliText)
        tvLatency = findViewById(R.id.tvLatency)
        tvModelStatus = findViewById(R.id.tvModelStatus)
        progressLoading = findViewById(R.id.progressLoading)
        btnStart = findViewById(R.id.btnStartTranslation)
        btnStop = findViewById(R.id.btnStopTranslation)

        audioRecorder = AudioRecorder()
        pipeline = VoiceTranslationPipeline()
        vad = VadProcessor()

        // Seed curriculum data
        CurriculumSeeder.seedIfEmpty(this)

        // Initialize models in background
        initializeModels()

        // Setup button listeners
        btnStart.setOnClickListener {
            startTranslation()
        }

        btnStop.setOnClickListener {
            stopTranslation()
        }
    }

    private fun initializeModels() {
        progressLoading.visibility = View.VISIBLE
        tvModelStatus.text = getString(R.string.models_loading)
        Log.i(TAG, "Starting model initialization...")

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    OnnxEngineManager.initializeSessions(this@TeacherActivity, "sat")
                    pipeline.initPhrasebook(this@TeacherActivity)
                }
                
                progressLoading.visibility = View.GONE

                // Show detailed per-component status
                val statusText = buildString {
                    appendLine("Model Status (${OnnxEngineManager.initTimeMs}ms):")
                    appendLine("  ASR: ${OnnxEngineManager.asrStatus}")
                    appendLine("  NMT: ${OnnxEngineManager.nmtStatus}")
                    appendLine("  TTS: ${OnnxEngineManager.ttsStatus}")
                }
                tvModelStatus.text = statusText
                Log.i(TAG, statusText)

                if (OnnxEngineManager.isInitialized) {
                    tvModelStatus.setTextColor(0xFF1976D2.toInt())
                    btnStart.isEnabled = true

                    // Warn if NMT is not available
                    if (!OnnxEngineManager.hasNmtModel()) {
                        tvStatus.text = "⚠️ NMT model not loaded — using phrasebook fallback"
                        tvStatus.setTextColor(0xFFFF8F00.toInt())
                    }
                } else {
                    tvModelStatus.text = "❌ No models loaded\n${OnnxEngineManager.getStatusSummary()}"
                    tvModelStatus.setTextColor(0xFFFF0000.toInt())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model init failed: ${e.message}", e)
                progressLoading.visibility = View.GONE
                tvModelStatus.text = "❌ Init error: ${e.message}"
                tvModelStatus.setTextColor(0xFFFF0000.toInt())
            }
        }
    }

    private fun startTranslation() {
        if (!OnnxEngineManager.isInitialized) {
            tvStatus.text = getString(R.string.models_error)
            return
        }

        isTranslating = true
        btnStart.isEnabled = false
        btnStop.isEnabled = true
        tvStatus.text = getString(R.string.translating)
        vad.reset()

        Log.i(TAG, "Translation started")

        audioRecorder.startRecording { audioChunk ->
            if (!isTranslating) return@startRecording

            // Run VAD
            if (vad.isSpeechDetected(audioChunk)) {
                // Process through pipeline
                lifecycleScope.launch {
                    try {
                        val result = withContext(Dispatchers.Default) {
                            pipeline.processAudioChunk(audioChunk)
                        }
                        
                        runOnUiThread {
                            tvHindiText.text = result.asrText.ifBlank { getString(R.string.waiting_speech) }
                            tvSantaliText.text = result.translatedText.ifBlank { getString(R.string.translation_appears) }
                            tvLatency.text = "Latency: ${result.latencyMs}ms | Source: ${result.translationSource}"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Pipeline error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun stopTranslation() {
        isTranslating = false
        audioRecorder.stopRecording()
        vad.reset()
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        tvStatus.text = getString(R.string.stopped)
        Log.i(TAG, "Translation stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTranslating) {
            audioRecorder.stopRecording()
        }
        OnnxEngineManager.release()
    }
}

