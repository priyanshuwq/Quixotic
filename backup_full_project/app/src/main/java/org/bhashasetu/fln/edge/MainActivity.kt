package org.bhashasetu.fln.edge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.bhashasetu.fln.edge.engine.NmtEngine
import org.bhashasetu.fln.edge.engine.OlckToDevaTransliterator
import org.bhashasetu.fln.edge.engine.RomanToDevanagari
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvSourceLabel: TextView
    private lateinit var tvSourceText: TextView
    private lateinit var tvTargetLabel: TextView
    private lateinit var tvTargetText: TextView
    private lateinit var tvDevanagariText: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var btnSpeak: ImageButton
    private lateinit var btnSwitch: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvOfflineStatus: TextView
    private lateinit var etTextInput: EditText
    private lateinit var btnTranslate: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isListening = false
    private var ttsReady = false
    private var offlineAsrReady = false

    private lateinit var nmtEngine: NmtEngine
    private lateinit var transliterator: OlckToDevaTransliterator

    private val handler = Handler(Looper.getMainLooper())
    private var translateRunnable: Runnable? = null
    private var lastHindiInput: String = ""
    private var lastSantaliDevanagari: String = ""

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 1001
        private const val DEBOUNCE_MS = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSourceLabel = findViewById(R.id.tvSourceLabel)
        tvSourceText = findViewById(R.id.tvSourceText)
        tvTargetLabel = findViewById(R.id.tvTargetLabel)
        tvTargetText = findViewById(R.id.tvTargetText)
        tvDevanagariText = findViewById(R.id.tvDevanagariText)
        btnMic = findViewById(R.id.btnMic)
        btnSpeak = findViewById(R.id.btnSpeak)
        btnSwitch = findViewById(R.id.btnSwitch)
        tvStatus = findViewById(R.id.tvStatus)
        tvOfflineStatus = findViewById(R.id.tvOfflineStatus)
        etTextInput = findViewById(R.id.etTextInput)
        btnTranslate = findViewById(R.id.btnTranslate)

        nmtEngine = NmtEngine(this)
        transliterator = OlckToDevaTransliterator()
        tts = TextToSpeech(this, this)

        btnMic.setOnClickListener { toggleListening() }
        btnSpeak.setOnClickListener { speakTranslation() }
        btnSwitch.setOnClickListener {
            tvSourceText.text = ""
            tvTargetText.text = ""
            tvDevanagariText.text = ""
            etTextInput.text.clear()
            tvStatus.text = "Hindi \u2192 Santali"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        }
        btnTranslate.setOnClickListener {
            val text = etTextInput.text.toString().trim()
            if (text.isNotEmpty()) {
                tvSourceText.text = text
                doTranslate(text)
            }
        }

        etTextInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""
                if (text.length >= 2) {
                    translateRunnable?.let { handler.removeCallbacks(it) }
                    translateRunnable = Runnable {
                        tvSourceText.text = text
                        doTranslate(text)
                    }
                    handler.postDelayed(translateRunnable!!, DEBOUNCE_MS)
                }
            }
        })

        checkOfflineCapabilities()
        requestAudioPermission()
    }

    private fun checkOfflineCapabilities() {
        offlineAsrReady = SpeechRecognizer.isRecognitionAvailable(this)
        if (offlineAsrReady) {
            tvOfflineStatus.text = "Offline ASR: Ready"
            tvOfflineStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            tvOfflineStatus.text = "Offline ASR: Not available - Use text input or download Hindi offline pack"
            tvOfflineStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    private fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermission()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this,
                "Offline speech not available.\n\nTo enable:\n1. Open Google app\n2. Settings > Voice > Offline speech recognition\n3. Download Hindi language pack\n\nOr use text input below.",
                Toast.LENGTH_LONG).show()
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                tvStatus.text = "Listening... speak Hindi"
                tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                btnMic.setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
            }

            override fun onBeginningOfSpeech() {
                tvStatus.text = "Hearing you..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                tvStatus.text = "Processing..."
                btnMic.setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
            }

            override fun onError(error: Int) {
                isListening = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try again or type below."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout. Try again or type below."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error. Check microphone."
                    SpeechRecognizer.ERROR_CLIENT -> "Client error. Try again."
                    SpeechRecognizer.ERROR_SERVER -> "Server error - offline pack may not be installed."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy. Wait."
                    else -> "Error $error. Use text input below."
                }
                tvStatus.text = msg
                tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                btnMic.setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    tvSourceText.text = spokenText
                    etTextInput.setText(spokenText)
                    doTranslate(spokenText)
                } else {
                    tvStatus.text = "No speech detected. Try again or type below."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra("android.speech.extra.PREFER_OFFLINE", true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            tvStatus.text = "Could not start speech. Use text input."
            tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
        }
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        btnMic.setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
    }

    private fun doTranslate(text: String) {
        val hindiInput = if (RomanToDevanagari.isRomanScript(text)) {
            RomanToDevanagari.convert(text)
        } else {
            text
        }

        lastHindiInput = hindiInput
        val result = nmtEngine.translate(hindiInput)

        lastSantaliDevanagari = result.devanagari
        tvTargetText.text = result.olChiki
        tvDevanagariText.text = result.devanagari
        tvStatus.text = "Done! Tap speaker to hear Santali."
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
    }

    private fun speakTranslation() {
        if (lastSantaliDevanagari.isBlank()) {
            Toast.makeText(this, "Translate something first.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!ttsReady) {
            Toast.makeText(this, "TTS loading... Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        tts?.let { engine ->
            var result = engine.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = engine.setLanguage(Locale("hi"))
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this,
                    "Hindi voice not installed.\n\nTo enable:\n1. Settings > System > Language & input\n2. Text-to-speech output\n3. Install Hindi voice data",
                    Toast.LENGTH_LONG).show()
            } else {
                engine.speak(lastSantaliDevanagari, TextToSpeech.QUEUE_FLUSH, null, "translation")
            }
        }
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translateRunnable?.let { handler.removeCallbacks(it) }
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Mic ready!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Mic needed for speech. Use text input.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
