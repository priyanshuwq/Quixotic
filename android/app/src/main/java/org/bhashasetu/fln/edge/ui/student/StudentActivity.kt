package org.bhashasetu.fln.edge.ui.student

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.bhashasetu.fln.edge.R
import org.bhashasetu.fln.edge.audio.AudioPlayer

class StudentActivity : AppCompatActivity() {
    private lateinit var audioPlayer: AudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        audioPlayer = AudioPlayer()

        val tvLesson = findViewById<TextView>(R.id.tvLessonContent)
        val btnPlay = findViewById<Button>(R.id.btnPlayAudio)
        val btnNext = findViewById<Button>(R.id.btnNextLesson)

        tvLesson.text = "Lesson content will appear here"

        btnPlay.setOnClickListener {
            // Play translated audio
        }

        btnNext.setOnClickListener {
            // Load next lesson
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.stopPlayback()
    }
}
