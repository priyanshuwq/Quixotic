#!/usr/bin/env python3
"""
Fine-tune NLLB-200 distilled for Hindi→Santali (and other tribal languages).

Usage:
    python train_nmt.py --languages sat
    python train_nmt.py --languages sat ho mnj --epochs 5
    python train_nmt.py --base-model facebook/nllb-200-distilled-600M --epochs 10
"""

import argparse
import os
import sys
from pathlib import Path

import torch
import numpy as np
from datasets import load_dataset, DatasetDict
from transformers import (
    AutoModelForSeq2SeqLM,
    AutoTokenizer,
    Seq2SeqTrainer,
    Seq2SeqTrainingArguments,
    DataCollatorForSeq2Seq,
)
import sacrebleu


# Language code mapping for NLLB-200
LANG_CODES = {
    "hi": "hin_Deva",
    "sat": "sat_Olck",
    "ho": "hoc_Latn",   # Ho (Munda family) — closest available
    "mnj": "mun_Latn",  # Mundari — closest available
    "en": "eng_Latn",
}


def load_parallel_data(data_dir: str, src_lang: str, tgt_lang: str) -> DatasetDict:
    """Load TSV files as HuggingFace Dataset."""
    data_files = {}
    for split, fname in [("train", "train.tsv"), ("validation", "val.tsv"), ("test", "test.tsv")]:
        fpath = os.path.join(data_dir, fname)
        if os.path.exists(fpath) and os.path.getsize(fpath) > 0:
            data_files[split] = fpath

    if "train" not in data_files:
        raise FileNotFoundError(f"No train.tsv found in {data_dir}")

    dataset = load_dataset("csv", data_files=data_files, sep="\t", header=None)
    dataset = dataset.rename_columns({"0": "src", "1": "tgt"})
    return dataset


def preprocess_function(examples, tokenizer, src_lang_code, tgt_lang_code, max_length=128):
    """Tokenize source and target texts with NLLB language tags."""
    tokenizer.src_lang = src_lang_code

    model_inputs = tokenizer(
        examples["src"],
        max_length=max_length,
        truncation=True,
        padding="max_length",
    )

    tokenizer.src_lang = tgt_lang_code
    labels = tokenizer(
        examples["tgt"],
        max_length=max_length,
        truncation=True,
        padding="max_length",
    )
    model_inputs["labels"] = labels["input_ids"]
    return model_inputs


def compute_metrics(eval_preds, tokenizer):
    """Compute BLEU and chrF++ scores."""
    preds, labels = eval_preds
    if isinstance(preds, tuple):
        preds = preds[0]

    decoded_preds = tokenizer.batch_decode(preds, skip_special_tokens=True)
    labels = np.where(labels != -100, labels, tokenizer.pad_token_id)
    decoded_labels = tokenizer.batch_decode(labels, skip_special_tokens=True)

    # Strip whitespace
    decoded_preds = [p.strip() for p in decoded_preds]
    decoded_labels = [l.strip() for l in decoded_labels]

    bleu = sacrebleu.corpus_bleu(decoded_preds, [decoded_labels])
    chrf = sacrebleu.corpus_chrf(decoded_preds, [decoded_labels])

    return {
        "bleu": round(bleu.score, 2),
        "chrf": round(chrf.score, 2),
    }


def train_language(
    model_name: str,
    data_dir: str,
    output_dir: str,
    src_lang: str,
    tgt_lang: str,
    epochs: int = 10,
    batch_size: int = 8,
    lr: float = 5e-5,
    gradient_accumulation_steps: int = 4,
):
    """Train NLLB for one language pair."""
    src_code = LANG_CODES.get(src_lang, src_lang)
    tgt_code = LANG_CODES.get(tgt_lang, tgt_lang)

    print(f"\n{'='*60}")
    print(f"Training: {src_lang} ({src_code}) → {tgt_lang} ({tgt_code})")
    print(f"Model: {model_name}")
    print(f"{'='*60}")

    # Load tokenizer & model
    print("Loading tokenizer and model...")
    tokenizer = AutoTokenizer.from_pretrained(model_name, src_lang=src_code)
    model = AutoModelForSeq2SeqLM.from_pretrained(model_name)

    # Get target language BOS token ID
    tgt_token_id = tokenizer.convert_tokens_to_ids(tgt_code)
    print(f"Target language token ID ({tgt_code}): {tgt_token_id}")

    # Load data
    print(f"Loading data from {data_dir}...")
    dataset = load_parallel_data(data_dir, src_lang, tgt_lang)
    print(f"  Train: {len(dataset['train'])} pairs")
    if "validation" in dataset:
        print(f"  Val:   {len(dataset['validation'])} pairs")
    if "test" in dataset:
        print(f"  Test:  {len(dataset['test'])} pairs")

    # Tokenize
    print("Tokenizing...")
    tokenized = dataset.map(
        lambda x: preprocess_function(x, tokenizer, src_code, tgt_code),
        batched=True,
        remove_columns=["src", "tgt"],
    )

    # Training args (optimized for CPU with limited data)
    effective_batch = batch_size * gradient_accumulation_steps
    print(f"Effective batch size: {effective_batch} (batch={batch_size} × accum={gradient_accumulation_steps})")

    training_args = Seq2SeqTrainingArguments(
        output_dir=output_dir,
        num_train_epochs=epochs,
        per_device_train_batch_size=batch_size,
        per_device_eval_batch_size=batch_size,
        gradient_accumulation_steps=gradient_accumulation_steps,
        learning_rate=lr,
        weight_decay=0.01,
        warmup_ratio=0.1,
        eval_strategy="epoch" if "validation" in dataset else "no",
        save_strategy="epoch",
        logging_steps=10,
        predict_with_generate=True,
        generation_max_length=128,
        generation_num_beams=4,
        fp16=False,  # CPU training
        bf16=False,
        gradient_checkpointing=True,  # Save memory
        load_best_model_at_end="validation" in dataset,
        metric_for_best_model="bleu" if "validation" in dataset else None,
        greater_is_better=True,
        report_to="none",
        seed=42,
        dataloader_num_workers=0,  # CPU-safe
    )

    # Trainer
    data_collator = DataCollatorForSeq2Seq(tokenizer, model=model)
    trainer = Seq2SeqTrainer(
        model=model,
        args=training_args,
        train_dataset=tokenized["train"],
        eval_dataset=tokenized.get("validation"),
        tokenizer=tokenizer,
        data_collator=data_collator,
        compute_metrics=lambda p: compute_metrics(p, tokenizer) if "validation" in dataset else None,
    )

    # Train
    print("\n🚀 Starting training...")
    trainer.train()

    # Save best model
    print(f"\n💾 Saving model to {output_dir}")
    trainer.save_model(output_dir)
    tokenizer.save_pretrained(output_dir)

    # Evaluate on test set
    if "test" in tokenized:
        print("\n📊 Evaluating on test set...")
        test_results = trainer.evaluate(tokenized["test"], metric_key_prefix="test")
        print(f"  Test BLEU: {test_results.get('test_bleu', 'N/A')}")
        print(f"  Test chrF: {test_results.get('test_chrf', 'N/A')}")

    return output_dir


def main():
    parser = argparse.ArgumentParser(description="Fine-tune NLLB-200 for Hindi→Tribal languages")
    parser.add_argument("--base-model", default="facebook/nllb-200-distilled-600M",
                       help="Base model to fine-tune")
    parser.add_argument("--data-root", default="data/raw",
                       help="Root directory with hi_<lang>/ subdirs")
    parser.add_argument("--output-root", default="models/finetuned",
                       help="Where to save fine-tuned models")
    parser.add_argument("--epochs", type=int, default=10)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--lr", type=float, default=5e-5, help="Learning rate")
    parser.add_argument("--grad-accum", type=int, default=4,
                       help="Gradient accumulation steps")
    parser.add_argument("--languages", nargs="+", default=["sat"],
                       help="Target languages to train")
    args = parser.parse_args()

    print("=" * 60)
    print("BhashaSetu NMT Training")
    print(f"  Base model:  {args.base_model}")
    print(f"  Languages:   {args.languages}")
    print(f"  Epochs:      {args.epochs}")
    print(f"  Batch size:  {args.batch_size}")
    print(f"  GPU:         {'✅ ' + torch.cuda.get_device_name() if torch.cuda.is_available() else '❌ CPU only'}")
    print("=" * 60)

    for tgt_lang in args.languages:
        data_dir = os.path.join(args.data_root, f"hi_{tgt_lang}")
        output_dir = os.path.join(args.output_root, f"nllb_hi_{tgt_lang}")

        train_path = os.path.join(data_dir, "train.tsv")
        if not os.path.exists(train_path) or os.path.getsize(train_path) < 100:
            print(f"\n⚠️  Skipping {tgt_lang}: No sufficient training data at {data_dir}")
            continue

        train_language(
            model_name=args.base_model,
            data_dir=data_dir,
            output_dir=output_dir,
            src_lang="hi",
            tgt_lang=tgt_lang,
            epochs=args.epochs,
            batch_size=args.batch_size,
            lr=args.lr,
            gradient_accumulation_steps=args.grad_accum,
        )

    print("\n✅ All training complete!")


if __name__ == "__main__":
    main()
