package org.bhashasetu.fln.edge.audio

import android.media.AudioFormat
import android.media.AudioTrack

class AudioPlayer {
    private var audioTrack: AudioTrack? = null

    companion object {
        const val SAMPLE_RATE = 22050
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    }

    fun playAudio(audioData: FloatArray) {
        val bufferSize = audioData.size * 4
        audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .setEncoding(AUDIO_FORMAT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        audioTrack?.play()
        audioTrack?.write(audioData, 0, audioData.size, AudioTrack.WRITE_BLOCKING)
    }

    fun stopPlayback() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
