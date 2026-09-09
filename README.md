# BhashaSetu

**AI-Powered Vernacular Pedagogy and Real-Time Translation Tool for Mother Tongue-Based Primary Education**

> SIH 2026 Problem Statement #26042 | Government of Jharkhand

## Overview

BhashaSetu is an AI-powered translation and pedagogy tool designed to enable non-native-speaking primary school teachers to deliver mother-tongue-based instruction in tribal languages (Ho, Mundari, Santhali) without prior language training.

### Key Features

- **Neural Machine Translation**: Hindi-to-tribal-language translation using DistilBERT-based NMT
- **Real-Time Voice Translation**: Sub-3-second latency voice-to-voice translation
- **Speech-to-Text (ASR)**: WHISPER-tiny based automatic speech recognition
- **Text-to-Speech (TTS)**: Festival-based synthesis for tribal languages
- **Offline-First Design**: Works on low-cost Android tablets (2GB RAM, Android 9+)
- **Bilingual Worksheet Generation**: Auto-generated worksheets aligned to NIPUN Bharat
- **Visual Flashcard Sets**: Context-aware flashcard generation

## Project Structure

```
bhashasetu/
├── data/                    # Datasets and vocabularies
│   ├── raw/                 # Raw parallel corpora
│   ├── processed/           # Preprocessed training data
│   ├── external/            # External data sources
│   └── vocabularies/        # Tokenizer vocabularies
├── models/                  # Model artifacts
│   ├── checkpoints/         # Training checkpoints
│   ├── exported/            # ONNX/TFLite exported models
│   ├── pretrained/          # Pretrained weights
│   └── quantized/           # INT8 quantized models
├── src/                     # Source code
│   ├── data/                # Data loading & processing
│   ├── models/              # Model architectures
│   ├── training/            # Training loops
│   ├── inference/           # Inference pipelines
│   ├── evaluation/          # Metrics & evaluation
│   └── utils/               # Utility functions
├── training/                # Training configs & scripts
├── api/                     # FastAPI backend
├── mobile/                  # Android app
├── notebooks/               # Jupyter notebooks
├── tests/                   # Unit & integration tests
├── configs/                 # Configuration files
├── scripts/                 # Utility scripts
└── outputs/                 # Logs, plots, metrics
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| NMT Model | DistilBERT + Madlad-400 |
| ASR | WHISPER-tiny |
| TTS | Festival-based synthesis |
| Optimization | INT8 ONNX Quantization |
| Backend | FastAPI |
| Mobile | Android (Kotlin/Java) |
| Training | PyTorch + HuggingFace |

## Quick Start

```bash
# Clone the repository
git clone https://github.com/your-org/bhashasetu.git
cd bhashasetu

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate  # Windows

# Install dependencies
pip install -r requirements.txt

# Download pretrained models
python scripts/download_models.py

# Start training
python training/scripts/train.py --config configs/nmt_config.yaml
```

## Target Languages

| Language | Family | Speakers |
|----------|--------|----------|
| Ho | Munda | ~1.4 million |
| Mundari | Munda | ~2 million |
| Santhali | Munda | ~7.4 million |

## Performance Metrics

- **Translation Accuracy**: 89% subject-accurate scores
- **Latency**: <3 seconds (voice-to-voice)
- **Model Size**: <500MB (quantized)
- **Memory**: <2GB RAM usage

## SIH 2026 Submission

- **Problem Statement**: SIH26042
- **Organization**: Government of Jharkhand
- **Theme**: Smart Education
- **Team**: Quixotic

## License

MIT License - See [LICENSE](LICENSE) for details.

## Contact

Team Quixotic - [Contact Information]
