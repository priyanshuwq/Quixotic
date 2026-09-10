#!/usr/bin/env python
"""Fine-tune MarianMT for Hindi→Tribal languages."""

import argparse
import os
import sys
from pathlib import Path

import torch
from datasets import load_dataset, DatasetDict
from transformers import (
    AutoModelForSeq2SeqLM,
    AutoTokenizer,
    Seq2SeqTrainer,
    Seq2SeqTrainingArguments,
    DataCollatorForSeq2Seq,
)
import sacrebleu
import numpy as np


def load_parallel_data(data_dir: str, src_lang: str, tgt_lang: str) -> DatasetDict:
    """Load TSV files as HuggingFace Dataset."""
    data_files = {
        "train": os.path.join(data_dir, "train.tsv"),
        "validation": os.path.join(data_dir, "val.tsv"),
        "test": os.path.join(data_dir, "test.tsv"),
    }
    dataset = load_dataset("csv", data_files=data_files, sep="\t", header=None)
    dataset = dataset.rename_columns({0: "src", 1: "tgt"})
    return dataset


def preprocess_function(examples, tokenizer, max_length=128):
    """Tokenize source and target texts."""
    model_inputs = tokenizer(
        examples["src"],
        max_length=max_length,
        truncation=True,
        padding="max_length",
    )
    labels = tokenizer(
        examples["tgt"],
        max_length=max_length,
        truncation=True,
        padding="max_length",
    )
    model_inputs["labels"] = labels["input_ids"]
    return model_inputs


def compute_metrics(eval_preds, tokenizer):
    """Compute BLEU score."""
    preds, labels = eval_preds
    if isinstance(preds, tuple):
        preds = preds[0]
    decoded_preds = tokenizer.batch_decode(preds, skip_special_tokens=True)
    labels = np.where(labels != -100, labels, tokenizer.pad_token_id)
    decoded_labels = tokenizer.batch_decode(labels, skip_special_tokens=True)

    bleu = sacrebleu.corpus_bleu(decoded_preds, [decoded_labels])
    return {"bleu": bleu.score}


def train_language(
    model_name: str,
    data_dir: str,
    output_dir: str,
    src_lang: str,
    tgt_lang: str,
    epochs: int = 10,
    batch_size: int = 16,
    lr: float = 3e-5,
):
    """Train MarianMT for one language pair."""

    print(f"\n{'='*60}")
    print(f"Training: {src_lang} → {tgt_lang}")
    print(f"{'='*60}")

    # Load tokenizer & model
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    tokenizer.src_lang = src_lang
    tokenizer.tgt_lang = tgt_lang
    model = AutoModelForSeq2SeqLM.from_pretrained(model_name)

    # Load data
    dataset = load_parallel_data(data_dir, src_lang, tgt_lang)
    tokenized = dataset.map(
        lambda x: preprocess_function(x, tokenizer),
        batched=True,
        remove_columns=["src", "tgt"],
    )

    # Training args
    training_args = Seq2SeqTrainingArguments(
        output_dir=output_dir,
        num_train_epochs=epochs,
        per_device_train_batch_size=batch_size,
        per_device_eval_batch_size=batch_size,
        learning_rate=lr,
        weight_decay=0.01,
        warmup_steps=500,
        evaluation_strategy="epoch",
        save_strategy="epoch",
        logging_steps=50,
        predict_with_generate=True,
        generation_max_length=128,
        generation_num_beams=4,
        fp16=torch.cuda.is_available(),
        load_best_model_at_end=True,
        metric_for_best_model="bleu",
        greater_is_better=True,
        report_to="none",
        seed=42,
    )

    # Trainer
    data_collator = DataCollatorForSeq2Seq(tokenizer, model=model)
    trainer = Seq2SeqTrainer(
        model=model,
        args=training_args,
        train_dataset=tokenized["train"],
        eval_dataset=tokenized["validation"],
        tokenizer=tokenizer,
        data_collator=data_collator,
        compute_metrics=lambda p: compute_metrics(p, tokenizer),
    )

    # Train
    trainer.train()

    # Save best model
    trainer.save_model(output_dir)
    tokenizer.save_pretrained(output_dir)

    # Evaluate on test set
    test_results = trainer.evaluate(tokenized["test"], metric_key_prefix="test")
    print(f"Test BLEU: {test_results.get('test_bleu', 'N/A')}")

    return output_dir


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-model", default="Helsinki-NLP/opus-mt-hi")
    parser.add_argument("--data-root", default="data/raw")
    parser.add_argument("--output-root", default="models/finetuned")
    parser.add_argument("--epochs", type=int, default=10)
    parser.add_argument("--batch-size", type=int, default=16)
    parser.add_argument("--lr", type=float, default=3e-5, help="Learning rate")
    parser.add_argument("--languages", nargs="+", default=["sat", "ho", "mnj"])
    args = parser.parse_args()

    for tgt_lang in args.languages:
        data_dir = os.path.join(args.data_root, f"hi_{tgt_lang}")
        output_dir = os.path.join(args.output_root, f"marian_hi_{tgt_lang}")

        if not os.path.exists(os.path.join(data_dir, "train.tsv")):
            print(f"⚠️  Skipping {tgt_lang}: No training data at {data_dir}")
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
        )

    print("\n✅ All training complete!")


if __name__ == "__main__":
    main()
