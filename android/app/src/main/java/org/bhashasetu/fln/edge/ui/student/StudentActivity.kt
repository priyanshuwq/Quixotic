package org.bhashasetu.fln.edge.ui.student

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bhashasetu.fln.edge.R
import org.bhashasetu.fln.edge.audio.AudioPlayer
import org.bhashasetu.fln.edge.data.NipunDatabase
import org.bhashasetu.fln.edge.engine.OnnxEngineManager
import org.bhashasetu.fln.edge.engine.VoiceTranslationPipeline

class StudentActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "StudentActivity"
    }

    private lateinit var audioPlayer: AudioPlayer
    private lateinit var tvLessonTitle: TextView
    private lateinit var tvLessonContent: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnReplay: Button
    private lateinit var btnNext: Button

    private var currentLessonIndex = 0
    private var lessons = listOf<org.bhashasetu.fln.edge.data.LessonScript>()
    private var currentAudioData: FloatArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        audioPlayer = AudioPlayer(sampleRate = 22050)
        tvLessonTitle = findViewById(R.id.tvLessonTitle)
        tvLessonContent = findViewById(R.id.tvLessonContent)
        btnPlay = findViewById(R.id.btnPlayAudio)
        btnReplay = findViewById(R.id.btnReplayAudio)
        btnNext = findViewById(R.id.btnNextLesson)

        // Load lessons from database
        loadLessons()

        btnPlay.setOnClickListener {
            playLessonAudio()
        }

        btnReplay.setOnClickListener {
            playLessonAudio()
        }

        btnNext.setOnClickListener {
            showNextLesson()
        }
    }

    private fun loadLessons() {
        lifecycleScope.launch {
            try {
                val db = NipunDatabase.getDatabase(this@StudentActivity)
                lessons = withContext(Dispatchers.IO) {
                    db.nipunDao().getLessons(2, "mathematics")
                }
                
                if (lessons.isNotEmpty()) {
                    showLesson(0)
                } else {
                    tvLessonTitle.text = "पाठ / Lesson"
                    tvLessonContent.text = getString(R.string.lesson_content_placeholder)
                }
            } catch (e: Exception) {
                tvLessonContent.text = "Error loading lessons: ${e.message}"
            }
        }
    }

    private fun showLesson(index: Int) {
        if (index < 0 || index >= lessons.size) return
        
        currentLessonIndex = index
        val lesson = lessons[index]
        tvLessonTitle.text = lesson.titleSantali
        tvLessonContent.text = lesson.contentSantali
    }

    private fun showNextLesson() {
        if (currentLessonIndex < lessons.size - 1) {
            showLesson(currentLessonIndex + 1)
        } else {
            Toast.makeText(this, "No more lessons", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playLessonAudio() {
        if (currentAudioData != null) {
            audioPlayer.playAudio(currentAudioData!!)
            return
        }

        // Generate audio using TTS
        lifecycleScope.launch {
            try {
                val textToSpeak = lessons.getOrNull(currentLessonIndex)?.contentSantali ?: return@launch
                
                val pipeline = VoiceTranslationPipeline()
                val audio = withContext(Dispatchers.Default) {
                    // Use TTS to generate audio from Santali text
                    val method = VoiceTranslationPipeline::class.java.getDeclaredMethod("runTTS", String::class.java)
                    method.isAccessible = true
                    method.invoke(pipeline, textToSpeak) as? FloatArray
                }
                
                if (audio != null && audio.isNotEmpty()) {
                    currentAudioData = audio
                    audioPlayer.playAudio(audio)
                } else {
                    Toast.makeText(this@StudentActivity, "TTS not available", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@StudentActivity, "Audio generation failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.stopPlayback()
    }
}
