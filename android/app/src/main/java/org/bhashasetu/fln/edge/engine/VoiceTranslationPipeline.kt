package org.bhashasetu.fln.edge.engine

import ai.onnxruntime.OnnxTensor
import java.nio.FloatBuffer

class VoiceTranslationPipeline {

    data class PipelineResult(
        val asrText: String,
        val translatedText: String,
        val synthesizedAudio: FloatArray,
        val latencyMs: Long
    )

    fun processAudioChunk(audioChunk: FloatArray): PipelineResult {
        val startTime = System.nanoTime()

        val asrText = runASR(audioChunk)
        val translatedText = runNMT(asrText)
        val synthesizedAudio = runTTS(translatedText)

        val latencyMs = (System.nanoTime() - startTime) / 1_000_000

        return PipelineResult(asrText, translatedText, synthesizedAudio, latencyMs)
    }

    private fun runASR(audioChunk: FloatArray): String {
        val session = OnnxEngineManager.getAsrSession()
        val inputShape = longArrayOf(1, audioChunk.size.toLong())
        val inputTensor = OnnxTensor.createTensor(session.environment, FloatBuffer.wrap(audioChunk), inputShape)
        val inputName = session.inputNames.first()
        val results = session.run(mapOf(inputName to inputTensor))
        val output = results.first().value as Array<Array<String>>
        inputTensor.close()
        return output[0][0]
    }

    private fun runNMT(hindiText: String): String {
        val session = OnnxEngineManager.getNmtSession()
        val tokens = hindiText.codePoints().toArray()
        val inputShape = longArrayOf(1, tokens.size.toLong())
        val inputTensor = OnnxTensor.createTensor(session.environment, tokens.map { it.toLong() }.toLongArray(), inputShape)
        val inputName = session.inputNames.first()
        val results = session.run(mapOf(inputName to inputTensor))
        val outputIds = results.first().value as Array<LongArray>
        inputTensor.close()
        return String(outputIds[0].map { it.toInt().toChar() }.toCharArray())
    }

    private fun runTTS(santaliText: String): FloatArray {
        val session = OnnxEngineManager.getTtsSession()
        val phonemes = santaliText.codePoints().toArray()
        val inputShape = longArrayOf(1, phonemes.size.toLong())
        val inputTensor = OnnxTensor.createTensor(session.environment, phonemes.map { it.toLong() }.toLongArray(), inputShape)
        val inputName = session.inputNames.first()
        val results = session.run(mapOf(inputName to inputTensor))
        val audioOutput = results.first().value as Array<FloatArray>
        inputTensor.close()
        return audioOutput[0]
    }
}
