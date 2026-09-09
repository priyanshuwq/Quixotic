#!/usr/bin/env python
"""Export trained models to ONNX format for Android deployment."""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import torch
from transformers import AutoModelForSeq2SeqLM, AutoTokenizer


def export_marianmt_to_onnx(model_name: str, output_path: str, quantize: bool = True):
    """Export MarianMT model to ONNX."""
    print(f"Loading model: {model_name}")
    
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    model = AutoModelForSeq2SeqLM.from_pretrained(model_name)
    model.eval()
    
    dummy_input = tokenizer("Hello world", return_tensors="pt", padding=True, truncation=True)
    
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    torch.onnx.export(
        model,
        (dummy_input["input_ids"], dummy_input["attention_mask"]),
        output_path,
        input_names=["input_ids", "attention_mask"],
        output_names=["logits"],
        dynamic_axes={
            "input_ids": {0: "batch_size", 1: "sequence"},
            "attention_mask": {0: "batch_size", 1: "sequence"},
            "logits": {0: "batch_size", 1: "sequence"},
        },
        opset_version=14,
    )
    
    print(f"Exported to: {output_path}")
    
    if quantize:
        from onnxruntime.quantization import quantize_dynamic, QuantType
        quantized_path = output_path.replace(".onnx", "_int8.onnx")
        quantize_dynamic(output_path, quantized_path, weight_type=QuantType.QInt8)
        print(f"Quantized to: {quantized_path}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="Helsinki-NLP/opus-mt-hi")
    parser.add_argument("--output", default="models/nmt/marian_hi_sat_int8.onnx")
    parser.add_argument("--no-quantize", action="store_true")
    args = parser.parse_args()
    
    export_marianmt_to_onnx(args.model, args.output, quantize=not args.no_quantize)


if __name__ == "__main__":
    main()
