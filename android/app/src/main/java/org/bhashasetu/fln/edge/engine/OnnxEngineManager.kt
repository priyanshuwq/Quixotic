package org.bhashasetu.fln.edge.engine

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.InputStream

object OnnxEngineManager {
    private var env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var asrSession: OrtSession? = null
    private var nmtSession: OrtSession? = null
    private var ttsSession: OrtSession? = null

    fun initializeSessions(context: Context) {
        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(1)
            setInterOpNumThreads(1)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            addNNAPI()
        }

        asrSession = env.createSession(loadModelBytes(context, "models/asr/whisper_tiny_int8.onnx"), sessionOptions)
        nmtSession = env.createSession(loadModelBytes(context, "models/nmt/marian_hi_sat_int8.onnx"), sessionOptions)
        ttsSession = env.createSession(loadModelBytes(context, "models/tts/piper_sat_int8.onnx"), sessionOptions)
    }

    private fun loadModelBytes(context: Context, fileName: String): ByteArray {
        val inputStream: InputStream = context.assets.open(fileName)
        val bytes = inputStream.readBytes()
        inputStream.close()
        return bytes
    }

    fun getAsrSession(): OrtSession = asrSession ?: throw IllegalStateException("ASR Session uninitialized")
    fun getNmtSession(): OrtSession = nmtSession ?: throw IllegalStateException("NMT Session uninitialized")
    fun getTtsSession(): OrtSession = ttsSession ?: throw IllegalStateException("TTS Session uninitialized")

    fun release() {
        asrSession?.close()
        nmtSession?.close()
        ttsSession?.close()
        env.close()
    }
}
