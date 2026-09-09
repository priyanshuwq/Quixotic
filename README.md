# BhashaSetu

**AI-Powered Vernacular Pedagogy and Real-Time Translation Tool for Mother Tongue-Based Primary Education**

> SIH 2026 Problem Statement #26042 | Government of Jharkhand

## Overview

BhashaSetu is an offline-first, edge-AI translation tool for primary school teachers in Jharkhand's tribal areas. It enables Hindi-speaking teachers to deliver mother-tongue-based instruction in Ho, Mundari, and Santhali using low-cost Android tablets.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    ANDROID TABLET (2GB RAM)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Audio Ingest] ──► [ASR] ──► [NMT] ──► [TTS] ──► [Playback]  │
│                      500ms    INT8       INT8      INT8         │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ WHISPER-Tiny│  │  MarianMT   │  │  Piper TTS  │            │
│  │  (INT8 ONNX)│  │ (INT8 ONNX) │  │ (INT8 ONNX) │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  Room Database                          │   │
│  │  NIPUN Milestones | Lessons | Worksheets | Flashcards   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  P2P Sync (Wi-Fi Direct)                │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
bhashasetu/
├── android/                    # Android app (PRIMARY)
│   ├── app/src/main/java/
│   │   └── org/bhashasetu/fln/edge/
│   │       ├── engine/         # ONNX inference pipeline
│   │       ├── data/           # Room database
│   │       ├── ui/             # Teacher/Student activities
│   │       ├── audio/          # AudioRecord/AudioTrack
│   │       ├── sync/           # P2P content sync
│   │       └── utils/          # Helpers
│   ├── app/src/main/assets/
│   │   └── models/             # ONNX models
│   └── app/build.gradle
│
├── training/                   # Python training (SECONDARY)
│   ├── configs/                # Model configurations
│   ├── data/                   # Training datasets
│   ├── models/                 # Checkpoints
│   └── scripts/                # Export scripts
│
├── models/                     # Deployed ONNX models
│   ├── asr/                    # WHISPER-Tiny INT8
│   ├── nmt/                    # MarianMT INT8
│   └── tts/                    # Piper TTS INT8
│
├── docs/                       # Documentation
└── README.md
```

## Tech Stack

| Component | Technology | Target |
|-----------|-----------|--------|
| ASR | WHISPER-Tiny | INT8 ONNX |
| NMT | MarianMT / NLLB-200 | INT8 ONNX |
| TTS | Piper TTS / VITS | INT8 ONNX |
| Runtime | ONNX Runtime Mobile | Android |
| Database | Room (SQLite) | On-device |
| Sync | Wi-Fi Direct P2P | Offline |
| Platform | Kotlin | Android 9+ |

## Performance Targets

| Metric | Threshold | Method |
|--------|-----------|--------|
| RAM Usage | ≤ 450 MB | Android Profiler |
| Inference Latency | ≤ 600 ms | System.nanoTime() |
| Thermal Delta | ≤ 4°C | BatteryManager |

## Target Languages

| Language | Code | Speakers |
|----------|------|----------|
| Hindi | hi | Base language |
| Ho | ho | ~1.4 million |
| Mundari | mnj | ~2 million |
| Santhali | sat | ~7.4 million |

## Hardware Requirements

- Android 9.0+ (API 28)
- 2GB RAM minimum
- 500MB free storage
- Speaker/Microphone

## Quick Start

### Android App
```bash
cd android
./gradlew assembleDebug
```

### Export Models
```bash
cd training
pip install -r requirements.txt
python scripts/export_to_onnx.py
```

## SIH 2026

- **Problem Statement**: SIH26042
- **Organization**: Government of Jharkhand
- **Theme**: Smart Education
- **Team**: Quixotic

## License

MIT
