#!/usr/bin/env python
"""Export trained model to ONNX format for mobile deployment."""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

import torch
from src.models.nmt import NMTModel


def export_to_onnx(
    model_path: str,
    output_path: str,
    quantize: bool = True,
):
    """Export NMT model to ONNX format."""
    print(f"Loading model from: {model_path}")
    
    # Load model
    checkpoint = torch.load(model_path, map_location="cpu")
    model = NMTModel()
    model.load_state_dict(checkpoint["model_state_dict"])
    model.eval()
    
    # Create dummy input
    batch_size = 1
    seq_len = 128
    dummy_input_ids = torch.randint(0, 250003, (batch_size, seq_len))
    dummy_attention_mask = torch.ones(batch_size, seq_len, dtype=torch.long)
    
    # Export
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    torch.onnx.export(
        model,
        (dummy_input_ids, dummy_attention_mask),
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
    
    print(f"Model exported to: {output_path}")
    
    # Quantize if requested
    if quantize:
        from onnxruntime.quantization import quantize_dynamic, QuantType
        
        quantized_path = output_path.replace(".onnx", "_quantized.onnx")
        quantize_dynamic(
            output_path,
            quantized_path,
            weight_type=QuantType.QInt8,
        )
        print(f"Quantized model saved to: {quantized_path}")


def main():
    parser = argparse.ArgumentParser(description="Export BhashaSetu model to ONNX")
    parser.add_argument("--model", required=True, help="Path to model checkpoint")
    parser.add_argument("--output", required=True, help="Output ONNX path")
    parser.add_argument("--no-quantize", action="store_true", help="Skip quantization")
    args = parser.parse_args()
    
    export_to_onnx(args.model, args.output, quantize=not args.no_quantize)


if __name__ == "__main__":
    main()
