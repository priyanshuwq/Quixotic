# Developer Implementation & Benchmarking Guide
## Project: BhashaSetu FLN Edge AI System
**Target Target Environment:** Android 9+ (API 28), <= 2GB RAM Hardware, 100% Offline, All-ONNX Pipeline.

---

## Module 1: Android System & Engine Initialization

### 1. Project Setup (`app/build.gradle`)
Your project must use native C++/Kotlin interoperability for raw memory control. Use the official Microsoft ONNX Runtime Mobile distribution.

```groovy
android {
    compileSdk 34
    defaultConfig {
        applicationId "org.bhashasetu.fln.edge"
        minSdk 28 // Android 9.0 Pie compliance
        targetSdk 34
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a' // Target low-cost ARM processors
        }
    }
}

dependencies {
    // Native ONNX Runtime Mobile wrapper
    implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.18.0'
    // Local SQLite database engine for NIPUN templates
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    // Audio manipulation libraries
    implementation 'androidx.media:media:1.7.0'
}
```

### 2. High-Efficiency ONNX Session Management (Kotlin Pattern)
To ensure the app does not hit an Out-Of-Memory (OOM) exception on 2GB RAM devices, implement the ONNX execution provider initialization using a **Singleton Lifecycle** with explicitly configured thread pools and CPU/NNAPI fallback logic.

```kotlin
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
            // Allocate strict resource limits for 2GB RAM
            setIntraOpNumThreads(1) 
            setInterOpNumThreads(1)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            
            // Enable Android NNAPI for hardware acceleration fallback
            addNNAPI() 
        }

        // Load models directly from compressed raw assets
        asrSession = env.createSession(loadModelBytes(context, "asr_whisper_tiny_int8.onnx"), sessionOptions)
        nmtSession = env.createSession(loadModelBytes(context, "nmt_marian_hi_sat_int8.onnx"), sessionOptions)
        ttsSession = env.createSession(loadModelBytes(context, "tts_vits_sat_fp16.onnx"), sessionOptions)
    }

    private fun loadModelBytes(context: Context, fileName: String): ByteArray {
        val inputStream: InputStream = context.assets.open("models/$fileName")
        val bytes = inputStream.readBytes()
        inputStream.close()
        return bytes
    }

    fun getAsrSession(): OrtSession = asrSession ?: throw IllegalStateException("ASR Session uninitialized")
    fun getNmtSession(): OrtSession = nmtSession ?: throw IllegalStateException("NMT Session uninitialized")
    fun getTtsSession(): OrtSession = ttsSession ?: throw IllegalStateException("TTS Session uninitialized")
}
```

---

## Module 2: The Core 3-Stage Pipeline Implementation

Your pipeline must read audio in real-time, execute model code sequentially, and instantly route the generated text into the next block.

```
[Audio Ingest 500ms Buffer] 
      │
      ▼ (Inference 1)
[ASR Engine] ──► Produces Hindi Token String
      │
      ▼ (Inference 2)
[NMT Engine] ──► Maps tokens to Santali Character Array
      │
      ▼ (Inference 3)
[TTS Engine] ──► Generates FloatArray PCM Data
      │
      ▼
[AudioTrack Playback] ──► Routes to Bluetooth/AUX Speaker
```

### Step 1: Speech-To-Text (ASR) Engine
*   **Model Source:** Quantize an open-source speech model like **Whisper-Tiny** or **Vosk** using Python's `onnxruntime.quantization`.
*   **Processing Instruction:** Open an Android `AudioRecord` thread pulling **16kHz Mono 16-bit PCM** data. Feed 500ms array windows into the ASR session. Close the input tensor immediately after token extraction.

### Step 2: Machine Translation (NMT) Engine
*   **Model Source:** Export an open-source sequence-to-sequence model (such as a distilled **MarianMT** or **NLLB-200 35M Parameter**) to ONNX format.
*   **Processing Instruction:** Pass the output text array from the ASR step through the NMT token matrix. Ensure the dictionary file handles both Devanagari Hindi strings and Santali (Ol Chiki or Latin representation) target strings.

### Step 3: Text-To-Speech (TTS) Engine
*   **Model Source:** A specialized acoustic model like **Piper TTS** or a lightweight **VITS** configured for Indian languages.
*   **Processing Instruction:** Feed the raw target characters from the NMT output into the TTS ONNX engine. Write the output `FloatArray` PCM data stream directly into a looping native Android `AudioTrack` object configured for low-latency playback.

---

## Module 3: NIPUN Bharat Database & File Sharing Architecture

Because the device operates completely offline, all data matching must look up hardcoded values using index schemas.

### 1. Database Layout (Room Persistence Framework)
Build out an optimized relational schema to link curriculum keywords to educational assets without running generative AI loops:

```kotlin
@Entity(tableName = "nipun_milestones")
data class NipunMilestone(
    @PrimaryKey val milestoneId: String,
    val developmentalGoal: Int, // 1, 2, or 3
    val competencyTextHindi: String,
    val competencyTextSantali: String
)

@Entity(tableName = "extractive_resources")
data class ExtractiveResource(
    @PrimaryKey val resourceId: String,
    val associatedMilestoneId: String,
    val keywordToken: String, // e.g., "counting", "hygiene"
    val summaryTextSantali: String,
    val assetPathFlashcard: String // Direct pointer to local .png file asset
)
```

### 2. P2P Sync & Offline Data Sharing
*   **Mechanism:** Implement **Wi-Fi Direct (P2P)** or an offline **Local Wi-Fi Network Server** on the Faculty App.
*   **Workflow:** The teacher switches on their device's native system hotspot functionality. The Student App connects to the local network gateway and executes a basic HTTP `GET /sync?student_id=XYZ` query to transfer the compressed `.m4a` audio lecture recordings and matching text summary packages.

---

## Module 4: Rigorous Quality Assurance & Performance Benchmarking

Your system must pass the following structural validation criteria to prevent deployment failure on target hardware:

| Metric | Threshold | Test Method |
| :--- | :--- | :--- |
| **Global RAM footprint** | <= 450 MB | Android Profiler Heap |
| **Inference Latency** | <= 600 ms Total Loop | System.nanoTime() |
| **Thermal Profile** | Max Delta 4°C over 1 hour | BatteryManager API |

### 1. Memory Verification Procedure
1. Run the application through the **Android Studio Profiler**.
2. Trigger the continuous audio pipeline loop for 45 minutes continuously.
3. **Pass Criteria:** The Java Heap must flatten out, demonstrating that the native C++ layers are successfully garbage collecting `OrtSession` vectors without memory leaks.

### 2. Computational Latency Validation
Wrap the main processing loops inside accurate timestamp variables:
```kotlin
val startTime = System.nanoTime()
// Run ASR -> NMT -> TTS Pipeline
val totalLatencyMs = (System.nanoTime() - startTime) / 1_000_000
Log.d("Benchmark", "Total Pipeline Latency: $totalLatencyMs ms")
```
*   **Pass Criteria:** `totalLatencyMs` must average under **600ms** per chunk to maintain smooth audio output over the classroom loudspeaker without noticeable stuttering.

---

## Module 5: Government Evaluation Framework & Unique Selling Propositions (USP)

When demonstrating this system to education departments, emphasize these core evaluation metrics:

### 1. Unmatched Cost Efficiency (Capex/Opex)
*   **The Problem:** Traditional AI initiatives require purchasing specialized hardware or paying recurring cloud API fees (like OpenAI or Google Cloud translation tokens).
*   **Our Solution:** The application operates entirely locally on entry-level, sub-₹8,000 tablets that regional governments already buy for public schools. Infrastructure operating costs are effectively zero because there are no cloud subscription overheads.

### 2. High Linguistic Reliability for Early Learners
*   **The Problem:** Standard translation models frequently inject literal English slang, which confuses primary school children who need correct native phonetics.
*   **Our Solution:** The app uses localized acoustic models tailored to the correct vowel lengths and retroflex sounds of regional tribal scripts. This ensures that the audio output broadcast over the classroom speaker remains clear and easy for students to understand.

### 3. Structural NIPUN Bharat Alignment
*   **The Problem:** Generic voice translation tools lack formal context and cannot assist teachers with classroom lesson plans.
*   **Our Solution:** The platform's on-device lookup architecture automatically indexes the teacher's lesson content directly to regional literacy goals. This enables teachers to fulfill national educational policy requirements entirely offline.
