package org.bhashasetu.fln.edge.ui.teacher

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.bhashasetu.fln.edge.R
import org.bhashasetu.fln.edge.audio.AudioRecorder
import org.bhashasetu.fln.edge.engine.OnnxEngineManager
import org.bhashasetu.fln.edge.engine.VoiceTranslationPipeline

class TeacherActivity : AppCompatActivity() {
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var pipeline: VoiceTranslationPipeline
    private var isTranslating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher)

        OnnxEngineManager.initializeSessions(this)
        pipeline = VoiceTranslationPipeline()
        audioRecorder = AudioRecorder()

        val btnStart = findViewById<Button>(R.id.btnStartTranslation)
        val btnStop = findViewById<Button>(R.id.btnStopTranslation)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvLatency = findViewById<TextView>(R.id.tvLatency)

        btnStart.setOnClickListener {
            isTranslating = true
            tvStatus.text = "Translating..."
            audioRecorder.startRecording { audioChunk ->
                val result = pipeline.processAudioChunk(audioChunk)
                runOnUiThread {
                    tvStatus.text = "Hindi: ${result.asrText}\nSantali: ${result.translatedText}"
                    tvLatency.text = "Latency: ${result.latencyMs}ms"
                }
            }
        }

        btnStop.setOnClickListener {
            isTranslating = false
            audioRecorder.stopRecording()
            tvStatus.text = "Stopped"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.stopRecording()
        OnnxEngineManager.release()
    }
}
