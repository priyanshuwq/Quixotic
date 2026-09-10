package org.bhashasetu.fln.edge.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun startRecording(onAudioChunk: (FloatArray) -> Unit) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid buffer size: $bufferSize")
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            audioRecord?.release()
            audioRecord = null
            return
        }

        isRecording = true
        audioRecord?.startRecording()
        Log.d(TAG, "Recording started")

        Thread {
            val buffer = ShortArray(bufferSize / 2)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val floatBuffer = FloatArray(read) {
                        buffer[it].toFloat() / 32768.0f
                    }
                    onAudioChunk(floatBuffer)
                }
            }
        }.start()
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Stop failed: ${e.message}")
        }
        audioRecord?.release()
        audioRecord = null
        Log.d(TAG, "Recording stopped")
    }
}
