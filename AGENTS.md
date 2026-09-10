# AGENTS.md

## Project Overview

BhashaSetu: Android-first Hindi→Tribal language translation app for Jharkhand teachers. Two codebases:
- `android/` — Kotlin Android app (primary)
- `training/` — Python model training/export (secondary)

## Build Commands

### Android
```bash
cd android && ./gradlew assembleDebug
```

### Python training
```bash
cd training && pip install -r requirements.txt
```

### Export models to ONNX
```bash
cd training && python scripts/export_to_onnx.py --model models/finetuned/nllb_hi_sat
```

## Architecture Notes

- **Entry point**: `TeacherActivity` (launcher) → `VoiceTranslationPipeline`
- **Pipeline**: Audio → ASR (Whisper-Tiny) → NMT (NLLB/MarianMT) → TTS (Piper)
- **Graceful degradation**: Phrasebook lookup when NMT model unavailable
- **ONNX models**: Load from `android/app/src/main/assets/models/`
- **Database**: Room DB for NIPUN curriculum milestones, lessons, worksheets

## Key Implementation Status

- ASR/NMT/TTS engines: Encoder exported, decoder loop **not yet implemented** (TODO in code)
- Phrasebook lookup: Working fallback for common classroom phrases
- P2P sync: Stubbed, not functional
- No test suite exists

## Constraints

- Target: Android 9+ (API 28), 2GB RAM devices
- ONNX INT8 quantization required for performance
- NDK filters: `armeabi-v7a`, `arm64-v8a` only
- Large model files gitignored: `models/**/*.onnx`, `models/**/*.bin`

## Conventions

- Kotlin with View Binding (no Compose)
- Coroutines for background work, `lifecycleScope` in Activities
- Dictionary/phrasebook JSONs in `android/app/src/main/assets/data/`
- Training data: TSV files with `src\tgt` columns
