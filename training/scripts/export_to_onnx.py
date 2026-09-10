#!/usr/bin/env python3
"""
Export fine-tuned NLLB model to ONNX with INT8 quantization for Android.

Produces:
- encoder_int8.onnx  (for Android assets/models/nmt/)
- decoder_int8.onnx  (for Android assets/models/nmt/)

Usage:
    python export_to_onnx.py --model models/finetuned/nllb_hi_sat
    python export_to_onnx.py --model models/finetuned/nllb_hi_sat --android-assets ../../android/app/src/main/assets/models/nmt
"""

import argparse
import os
import shutil
from pathlib import Path

import torch
import torch.nn as nn
from transformers import AutoModelForSeq2SeqLM, AutoTokenizer


def export_encoder(model, tokenizer, output_dir, max_length=128):
    """Export the encoder to ONNX."""
    print("  Exporting encoder...")
    encoder = model.get_encoder()
    encoder.eval()

    dummy_input_ids = torch.randint(0, tokenizer.vocab_size, (1, max_length))
    dummy_attention_mask = torch.ones(1, max_length, dtype=torch.long)

    output_path = os.path.join(output_dir, "encoder.onnx")
    torch.onnx.export(
        encoder,
        (dummy_input_ids, dummy_attention_mask),
        output_path,
        input_names=["input_ids", "attention_mask"],
        output_names=["last_hidden_state"],
        dynamic_axes={
            "input_ids": {0: "batch_size", 1: "seq_length"},
            "attention_mask": {0: "batch_size", 1: "seq_length"},
            "last_hidden_state": {0: "batch_size", 1: "seq_length"},
        },
        opset_version=14,
        do_constant_folding=True,
    )
    print(f"    Saved: {output_path}")
    return output_path


def export_decoder(model, tokenizer, output_dir, max_length=128):
    """Export the decoder to ONNX (simplified greedy decoder)."""
    print("  Exporting decoder...")

    class DecoderWrapper(nn.Module):
        """Wraps the decoder for ONNX export with encoder hidden states input."""
        def __init__(self, model):
            super().__init__()
            self.model = model
            self.decoder = model.get_decoder()
            self.lm_head = model.lm_head
            self.final_logits_bias = getattr(model, "final_logits_bias", None)

        def forward(self, decoder_input_ids, encoder_hidden_states, encoder_attention_mask):
            decoder_outputs = self.decoder(
                input_ids=decoder_input_ids,
                encoder_hidden_states=encoder_hidden_states,
                encoder_attention_mask=encoder_attention_mask,
            )
            logits = self.lm_head(decoder_outputs.last_hidden_state)
            if self.final_logits_bias is not None:
                logits = logits + self.final_logits_bias
            return logits

    decoder_wrapper = DecoderWrapper(model)
    decoder_wrapper.eval()

    hidden_size = model.config.d_model
    dummy_decoder_ids = torch.randint(0, tokenizer.vocab_size, (1, 1))
    dummy_encoder_hidden = torch.randn(1, max_length, hidden_size)
    dummy_encoder_mask = torch.ones(1, max_length, dtype=torch.long)

    output_path = os.path.join(output_dir, "decoder.onnx")
    torch.onnx.export(
        decoder_wrapper,
        (dummy_decoder_ids, dummy_encoder_hidden, dummy_encoder_mask),
        output_path,
        input_names=["decoder_input_ids", "encoder_hidden_states", "encoder_attention_mask"],
        output_names=["logits"],
        dynamic_axes={
            "decoder_input_ids": {0: "batch_size", 1: "dec_length"},
            "encoder_hidden_states": {0: "batch_size", 1: "enc_length"},
            "encoder_attention_mask": {0: "batch_size", 1: "enc_length"},
            "logits": {0: "batch_size", 1: "dec_length"},
        },
        opset_version=14,
        do_constant_folding=True,
    )
    print(f"    Saved: {output_path}")
    return output_path


def quantize_to_int8(onnx_path: str) -> str:
    """Quantize ONNX model to INT8 using onnxruntime quantization."""
    from onnxruntime.quantization import quantize_dynamic, QuantType

    output_path = onnx_path.replace(".onnx", "_int8.onnx")
    print(f"  Quantizing to INT8: {output_path}")

    quantize_dynamic(
        onnx_path,
        output_path,
        weight_type=QuantType.QInt8,
    )

    # Report size reduction
    orig_size = os.path.getsize(onnx_path) / (1024 * 1024)
    quant_size = os.path.getsize(output_path) / (1024 * 1024)
    print(f"    Original: {orig_size:.1f} MB")
    print(f"    Quantized: {quant_size:.1f} MB ({(quant_size/orig_size)*100:.0f}%)")

    return output_path


def main():
    parser = argparse.ArgumentParser(description="Export NLLB model to ONNX INT8")
    parser.add_argument("--model", required=True, help="Path to fine-tuned model")
    parser.add_argument("--output", default=None, help="Output directory (default: models/exported/<model_name>)")
    parser.add_argument("--android-assets", default=None,
                       help="Copy INT8 models to Android assets dir")
    parser.add_argument("--max-length", type=int, default=128, help="Max sequence length")
    args = parser.parse_args()

    model_path = args.model
    output_dir = args.output or os.path.join("models", "exported", Path(model_path).name)
    os.makedirs(output_dir, exist_ok=True)

    print("=" * 60)
    print("NLLB → ONNX INT8 Export")
    print(f"  Model: {model_path}")
    print(f"  Output: {output_dir}")
    print("=" * 60)

    # Load model
    print("\nLoading model and tokenizer...")
    tokenizer = AutoTokenizer.from_pretrained(model_path)
    model = AutoModelForSeq2SeqLM.from_pretrained(model_path)
    model.eval()

    # Export
    print("\n[1/3] Exporting to ONNX...")
    encoder_path = export_encoder(model, tokenizer, output_dir, args.max_length)
    decoder_path = export_decoder(model, tokenizer, output_dir, args.max_length)

    # Quantize
    print("\n[2/3] Quantizing to INT8...")
    encoder_int8 = quantize_to_int8(encoder_path)
    decoder_int8 = quantize_to_int8(decoder_path)

    # Copy to Android assets
    if args.android_assets:
        print(f"\n[3/3] Copying to Android assets: {args.android_assets}")
        os.makedirs(args.android_assets, exist_ok=True)
        shutil.copy2(encoder_int8, os.path.join(args.android_assets, "encoder_int8.onnx"))
        shutil.copy2(decoder_int8, os.path.join(args.android_assets, "decoder_int8.onnx"))

        # Also copy tokenizer files for the Android tokenizer
        for fname in ["dict.SRC.json", "dict.TGT.json", "sentencepiece.bpe.model"]:
            src = os.path.join(model_path, fname)
            if os.path.exists(src):
                shutil.copy2(src, os.path.join(args.android_assets, fname))
                print(f"    Copied: {fname}")

        print(f"    ✅ Models deployed to {args.android_assets}")
    else:
        print("\n[3/3] Skipping Android deployment (use --android-assets to deploy)")

    # Clean up FP32 files
    os.remove(encoder_path)
    os.remove(decoder_path)
    print(f"\n✅ Export complete!")
    print(f"   INT8 encoder: {encoder_int8}")
    print(f"   INT8 decoder: {decoder_int8}")


if __name__ == "__main__":
    main()
