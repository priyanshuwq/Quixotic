# BhashaSetu Developer Guide

## Setup

### Android Development
1. Install Android Studio
2. Open `android/` folder
3. Sync Gradle
4. Run on device/emulator

### Model Training
1. Create virtual environment:
   ```bash
   cd training
   python -m venv venv
   source venv/bin/activate  # Linux/Mac
   venv\Scripts\activate     # Windows
   ```
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Adding Models

### ASR Model
1. Download WHISPER-Tiny weights
2. Export to ONNX:
   ```python
   import whisper
   model = whisper.load_model("tiny")
   # Use torch.onnx.export
   ```
3. Quantize to INT8
4. Place in `models/asr/whisper_tiny_int8.onnx`
5. Copy to `android/app/src/main/assets/models/asr/`

### NMT Model
1. Train or download MarianMT
2. Export using `training/scripts/export_to_onnx.py`
3. Quantize to INT8
4. Place in `models/nmt/marian_hi_sat_int8.onnx`
5. Copy to `android/app/src/main/assets/models/nmt/`

### TTS Model
1. Train or download Piper TTS
2. Export to ONNX
3. Place in `models/tts/piper_sat_int8.onnx`
4. Copy to `android/app/src/main/assets/models/tts/`

## Database

### NIPUN Curriculum Data
1. Prepare JSON/CSV with milestones
2. Import to Room database
3. Schema in `data/Entities.kt`

## Testing

### Android Tests
```bash
cd android
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumentation tests
```

### Performance Testing
```kotlin
val benchmark = PerformanceBenchmark()
val result = benchmark.runBenchmark(pipeline, testChunks)
assert(result.passed)
```

## Deployment

1. Build release APK:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```
2. Sign APK
3. Distribute via USB or P2P sync

## Troubleshooting

### OOM on 2GB Device
- Reduce `setIntraOpNumThreads` to 1
- Use INT8 quantized models
- Monitor with Android Profiler

### High Latency
- Check model quantization
- Reduce audio chunk size
- Profile with System.nanoTime()
