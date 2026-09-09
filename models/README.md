# BhashaSetu Models

## Model Directory Structure

```
models/
├── checkpoints/      # Training checkpoints (.pt files)
├── exported/         # ONNX exported models
├── pretrained/       # Pretrained model weights
└── quantized/        # INT8 quantized models for mobile
```

## Model Files

| Model | Size | Format | Purpose |
|-------|------|--------|---------|
| nmt_base.pt | ~500MB | PyTorch | Base NMT model |
| nmt_quantized.onnx | ~125MB | ONNX INT8 | Mobile inference |
| whisper_tiny.pt | ~75MB | PyTorch | ASR model |

## Downloading Models

```bash
python scripts/download_models.py
```

## Exporting to ONNX

```bash
python training/scripts/export_onnx.py \
    --model models/checkpoints/best.pt \
    --output models/exported/nmt.onnx
```

## Model Usage

```python
from src.models import NMTModel
from src.inference import TranslationPipeline

model = NMTModel.from_pretrained("models/checkpoints/best.pt")
pipeline = TranslationPipeline(model, target_lang="sat")
result = pipeline.translate("नमस्ते दुनिया")
```

## Quantization

Models are quantized using ONNX Runtime for CPU inference:

- **Format**: INT8 dynamic quantization
- **Size reduction**: ~75%
- **Speed improvement**: ~2-3x on CPU
- **Accuracy loss**: <2%
