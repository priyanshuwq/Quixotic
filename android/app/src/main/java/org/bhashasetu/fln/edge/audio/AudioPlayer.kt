package org.bhashasetu.fln.edge.audio

import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

class AudioPlayer(private val sampleRate: Int = 22050) {
    private var audioTrack: AudioTrack? = null

    companion object {
        private const val TAG = "AudioPlayer"
    }

    fun playAudio(audioData: FloatArray) {
        if (audioData.isEmpty()) {
            Log.w(TAG, "Empty audio data")
            return
        }

        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
        val bufferSize = audioData.size * 4

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.write(audioData, 0, audioData.size, AudioTrack.WRITE_BLOCKING)
            audioTrack?.play()
            Log.d(TAG, "Playing audio: ${audioData.size} samples at ${sampleRate}Hz")
        } catch (e: Exception) {
            Log.e(TAG, "Playback failed: ${e.message}")
            stopPlayback()
        }
    }

    fun stopPlayback() {
        try {
            audioTrack?.stop()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Stop failed: ${e.message}")
        }
        audioTrack?.release()
        audioTrack = null
    }
}
