#!/usr/bin/env python
"""Download and convert models for BhashaSetu Android deployment."""

import os
import sys
import urllib.request
import json

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))


def download_whisper_tiny():
    """Download pre-quantized Whisper Tiny from sherpa-onnx."""
    base_url = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main"
    out_dir = os.path.join(PROJECT_ROOT, "android/app/src/main/assets/models/asr")
    os.makedirs(out_dir, exist_ok=True)

    files = ["tiny-encoder.int8.onnx", "tiny-decoder.int8.onnx", "tiny-tokens.txt"]
    for f in files:
        url = f"{base_url}/{f}"
        out = os.path.join(out_dir, f)
        if not os.path.exists(out):
            print(f"Downloading {f}...")
            urllib.request.urlretrieve(url, out)
            print(f"  -> {os.path.getsize(out)} bytes")
        else:
            print(f"  {f} already exists ({os.path.getsize(out)} bytes)")


def download_indictrans2_tokenizer():
    """Download IndicTrans2 tokenizer files (requires HF auth token)."""
    import getpass
    base_url = "https://huggingface.co/ai4bharat/indictrans2-indic-indic-dist-320M/resolve/main"
    out_dir = os.path.join(PROJECT_ROOT, "android/app/src/main/assets/models/nmt")
    os.makedirs(out_dir, exist_ok=True)

    token = os.environ.get("HF_TOKEN") or os.environ.get("HUGGING_FACE_HUB_TOKEN")
    if not token:
        token = getpass.getpass("HuggingFace token (for gated IndicTrans2 model): ")

    files = ["tokenizer.json", "dict.SRC.json", "dict.TGT.json"]
    for f in files:
        url = f"{base_url}/{f}"
        out = os.path.join(out_dir, f)
        if not os.path.exists(out) or os.path.getsize(out) < 1000:
            print(f"Downloading {f}...")
            try:
                req = urllib.request.Request(url)
                req.add_header("Authorization", f"Bearer {token}")
                with urllib.request.urlopen(req) as resp, open(out, "wb") as fp:
                    fp.write(resp.read())
                print(f"  -> {os.path.getsize(out)} bytes")
            except Exception as e:
                print(f"  Warning: {e}")
                if os.path.exists(out):
                    os.remove(out)


def main():
    os.chdir(PROJECT_ROOT)
    print("=== BhashaSetu Model Downloader ===\n")

    print("[1/2] Whisper Tiny ASR (multilingual)")
    download_whisper_tiny()

    print("\n[2/2] IndicTrans2 NMT tokenizer")
    try:
        download_indictrans2_tokenizer()
    except Exception as e:
        print(f"  Skipping NMT: {e}")
        print("  (Model is gated - provide HF_TOKEN env var or run manually with auth)")

    print("\nDownload complete!")


if __name__ == "__main__":
    main()
